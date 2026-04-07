package com.BTL_JAVA.BTL.Service.Product;

import com.BTL_JAVA.BTL.DTO.Request.ApiResponse;
import com.BTL_JAVA.BTL.DTO.Request.Sales.SalesCreationRequest;
import com.BTL_JAVA.BTL.DTO.Request.Sales.SalesUpdateRequest;
import com.BTL_JAVA.BTL.DTO.Request.Sales.ProductSaleItemRequest;
import com.BTL_JAVA.BTL.DTO.Response.Product.ProductResponse;
import com.BTL_JAVA.BTL.DTO.Response.Product.ProductVariationResponse;
import com.BTL_JAVA.BTL.DTO.Response.Sales.SalesResponse;
import com.BTL_JAVA.BTL.DTO.Response.Product.ProductSaleItemResponse;
import com.BTL_JAVA.BTL.Entity.Product.Sales;
import com.BTL_JAVA.BTL.Entity.Product.Product;
import com.BTL_JAVA.BTL.Entity.Product.ProductSale;
import com.BTL_JAVA.BTL.Repository.SalesRepository;
import com.BTL_JAVA.BTL.Repository.ProductRepository;
import com.BTL_JAVA.BTL.Repository.ProductSaleRepository;
import com.BTL_JAVA.BTL.Exception.AppException;
import com.BTL_JAVA.BTL.Exception.ErrorCode;
import com.BTL_JAVA.BTL.Service.RedisService;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SalesService {

    SalesRepository salesRepository;
    ProductRepository productRepository;
    ProductSaleRepository productSaleRepository;

    RedisService redisService;
    RedissonClient redissonClient;

    static String DISCOUNT_CACHE_PREFIX = "product:discount:";
    static String SALE_LIST_CACHE = "sale:list:all";
    static String DISCOUNT_LOCK_PREFIX = "lock:discount:";
    static String LIST_LOCK = "lock:sale:list";
    static String NULL_VALUE = "NOT_EXIST";

    public ApiResponse<List<SalesResponse>> getAllSales(Boolean active) {

        // Lấy dữ liệu từ cache
        List<SalesResponse> cacheList = redisService.getList(SALE_LIST_CACHE, new TypeReference<>(){});

        // Cache miss
        if(cacheList == null){
            RLock lock = redissonClient.getLock(LIST_LOCK);
            try{
                if(lock.tryLock(5, TimeUnit.SECONDS)){
                    try{
                        // Double check
                        cacheList = redisService.getList(SALE_LIST_CACHE, new TypeReference<>(){});

                        if(cacheList == null){
                            log.info("Cache miss for sales list, reading from DB...");
                            List<Sales> allSales = salesRepository.findAll();

                            cacheList = allSales.stream()
                                    .map(sale -> {
                                        boolean isActive = calculateActiveStatus(sale.getStDate(), sale.getEndDate());
                                        return toResponse(sale, isActive);
                                    })
                                    .toList();

                            redisService.set(SALE_LIST_CACHE, cacheList, Duration.ofMinutes(30));
                        }
                    } finally {
                        if(lock.isHeldByCurrentThread()){
                            lock.unlock();
                        }
                    }
                } else {
                    throw new AppException(ErrorCode.SYSTEM_BUSY);
                }
            } catch (InterruptedException e){
                Thread.currentThread().interrupt();
                throw new AppException(ErrorCode.SYSTEM_ERROR);
            }
        }

        // Tính toán lại trạng thái và filter trên ram
        List<SalesResponse> result = cacheList.stream()
                .map(sale -> {
                    sale.setActive(calculateActiveStatus(sale.getStDate(), sale.getEndDate()));
                    return sale;
                })
                .filter(sale -> active == null || sale.getActive().equals(active))
                .toList();

        return ApiResponse.<List<SalesResponse>>builder()
                .result(result)
                .build();

    }

    public Map<Integer, BigDecimal> getLastestDiscountMap(List<Integer> productIds){

        Map<Integer, BigDecimal> resultMap = new HashMap<>();
        if(productIds == null || productIds.isEmpty()) return resultMap;

        // Lấy dữ liệu từ cache
        List<String> keys = productIds.stream()
                .map(id -> DISCOUNT_CACHE_PREFIX + id)
                .toList();
        List<String> cacheValues = redisService.multiGet(keys);

        List<Integer> missIds = new ArrayList<>();

        // Validate product không tồn tại
        for(int i = 0; i < productIds.size(); i++){
            Integer productId = productIds.get(i);
            String cacheValue = cacheValues != null ? cacheValues.get(i) : null;

            if(cacheValue != null){
                if(NULL_VALUE.equals(cacheValue)){
                    log.warn("Product ID {} marked as NOT EXIST in cache", productId);
                    continue;
                }
                resultMap.put(productId, new BigDecimal(cacheValue.replace("\"", "")));
            }
            else{
                missIds.add(productId);
            }
        }

        // Cache miss
        if(!missIds.isEmpty()){
            log.info("Cache miss for {} products, performing Batch DB Fetch...", missIds.size());

            List<Product> existingProducts = productRepository.findAllById(missIds);
            Set<Integer> existingIds = existingProducts.stream()
                    .map(Product::getProductId)
                    .collect(Collectors.toSet());

            missIds.stream()
                    .filter(id -> !existingIds.contains(id))
                    .forEach(id -> redisService.set(DISCOUNT_CACHE_PREFIX + id, NULL_VALUE, Duration.ofMinutes(1)));

            if(!existingIds.isEmpty()){
                LocalDateTime now = LocalDateTime.now();
                List<ProductSale> activeProductSales = productSaleRepository
                        .findActiveSalesByProductIds(new ArrayList<>(existingIds), now);

                Map<Integer, BigDecimal> dbDiscountMap = activeProductSales.stream()
                        .collect(Collectors.toMap(
                                ps -> ps.getProduct().getProductId(),
                                ProductSale::getSaleValue,
                                (v1, v2) -> v1.compareTo(v2) > 0 ? v1 : v2
                        ));

                for(Integer pId : existingIds){
                    BigDecimal discount = dbDiscountMap.getOrDefault(pId, BigDecimal.ZERO);
                    resultMap.put(pId, discount);

                    String cacheKey = DISCOUNT_CACHE_PREFIX + pId;
                    redisService.set(cacheKey, discount, Duration.ofMinutes(30));
                }
            }
        }

        return resultMap;

    }

    @Transactional
    public ApiResponse<SalesResponse> create(SalesCreationRequest request) {
        try {
            validateSaleDates(request.getStDate(), request.getEndDate());
            validateNoOverlappingSales(request.getStDate(), request.getEndDate(), null);
            boolean isActive = calculateActiveStatus(request.getStDate(), request.getEndDate());

            Sales sale = Sales.builder()
                    .name(request.getName())
                    .description(request.getDescription() != null ? request.getDescription() : "")
                    .stDate(request.getStDate())
                    .endDate(request.getEndDate())
                    .active(isActive)
                    .build();

            Sales saved = salesRepository.save(sale);

            return ApiResponse.<SalesResponse>builder()
                    .result(toResponse(saved, isActive))
                    .build();
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException(ErrorCode.CREATE_FAILED);
        }
    }

    @Transactional
    public ApiResponse<SalesResponse> update(Integer id, SalesUpdateRequest request) {
        try {
            Sales sale = salesRepository.findById(id)
                    .orElseThrow(() -> new AppException(ErrorCode.SALE_NOT_EXISTED));

            List<Integer> affectedProductIds = new ArrayList<>();
            if(sale.getProductSales() != null){
                sale.getProductSales().forEach(ps ->
                        affectedProductIds.add(ps.getProduct().getProductId()));
            }

            if (request.getStDate() != null || request.getEndDate() != null) {
                LocalDateTime newStDate = request.getStDate() != null ? request.getStDate() : sale.getStDate();
                LocalDateTime newEndDate = request.getEndDate() != null ? request.getEndDate() : sale.getEndDate();
                validateNoOverlappingSales(newStDate, newEndDate, id);
            }
            if (request.getName() != null) sale.setName(request.getName());
            if (request.getDescription() != null) sale.setDescription(request.getDescription());
            if (request.getStDate() != null) sale.setStDate(request.getStDate());
            if (request.getEndDate() != null) sale.setEndDate(request.getEndDate());

            // VALIDATE DATES
            if (request.getStDate() != null && request.getEndDate() != null) {
                validateSaleDates(request.getStDate(), request.getEndDate());
            }

            // TÍNH LẠI ACTIVE STATUS
            boolean isActive = calculateActiveStatus(
                    request.getStDate() != null ? request.getStDate() : sale.getStDate(),
                    request.getEndDate() != null ? request.getEndDate() : sale.getEndDate()
            );
            sale.setActive(isActive);

            Sales saved = salesRepository.save(sale);

            // XỬ LÝ XÓA PRODUCTS
            if (request.getRemoveProductIds() != null && !request.getRemoveProductIds().isEmpty()) {
                removeProductsFromSale(saved, request.getRemoveProductIds());
            }

            // XỬ LÝ THÊM PRODUCTS MỚI
            if (request.getAddProducts() != null && !request.getAddProducts().isEmpty()) {
                addProductsToSale(saved, request.getAddProducts());

                affectedProductIds.addAll(
                        request.getAddProducts().stream()
                                .map(ProductSaleItemRequest::getProductId)
                                .toList()
                );
            }

            clearCache(affectedProductIds);

            return ApiResponse.<SalesResponse>builder()
                    .result(toResponse(saved, isActive))
                    .build();
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException(ErrorCode.UPDATE_FAILED);
        }
    }

    @Transactional
    public ApiResponse<Void> delete(Integer id) {
        try {
            Sales sale = salesRepository.findById(id)
                    .orElseThrow(() -> new AppException(ErrorCode.SALE_NOT_EXISTED));

            List<Integer> affectedIds = sale.getProductSales().stream()
                    .map(ps -> ps.getProduct().getProductId())
                    .toList();

            if (!sale.getProductSales().isEmpty()) {
                productSaleRepository.deleteAll(sale.getProductSales());
            }

            salesRepository.delete(sale);

            clearCache(affectedIds);

            return ApiResponse.<Void>builder()
                    .result(null)
                    .build();
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException(ErrorCode.DELETE_FAILED);
        }
    }
    private void validateNoOverlappingSales(LocalDateTime stDate, LocalDateTime endDate, Integer excludeSaleId) {
        List<Sales> allSales = salesRepository.findAll();

        boolean hasOverlap = allSales.stream()
                .filter(sale -> excludeSaleId == null || sale.getId() != excludeSaleId)
                .anyMatch(sale -> isTimeOverlapping(stDate, endDate, sale.getStDate(), sale.getEndDate()));

        if (hasOverlap) {
            throw new AppException(ErrorCode.SALE_OVERLAPPING);
        }
    }

    private boolean isTimeOverlapping(LocalDateTime newSt, LocalDateTime newEnd, LocalDateTime existingSt, LocalDateTime existingEnd) {
        return (newSt.isBefore(existingEnd) && newEnd.isAfter(existingSt));
    }
    private void addProductsToSale(Sales sale, List<ProductSaleItemRequest> addProducts) {
        for (ProductSaleItemRequest item : addProducts) {
            boolean alreadyExists = sale.getProductSales().stream()
                    .filter(ps -> ps.getProduct() != null)
                    .anyMatch(ps -> item.getProductId().equals(ps.getProduct().getProductId()));

            if (alreadyExists) {
                ProductSale existing = sale.getProductSales().stream()
                        .filter(ps -> ps.getProduct() != null)
                        .filter(ps -> item.getProductId().equals(ps.getProduct().getProductId()))
                        .findFirst()
                        .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_IN_SALE));
                existing.setSaleValue(item.getValue());
            } else {
                Product product = productRepository.findById(item.getProductId())
                        .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

                validateSaleValue(item.getValue());

                ProductSale productSale = ProductSale.builder()
                        .sale(sale)
                        .product(product)
                        .saleValue(item.getValue())
                        .build();

                productSaleRepository.save(productSale);
                sale.getProductSales().add(productSale);
            }
        }
    }

    private void removeProductsFromSale(Sales sale, List<Integer> removeProductIds) {
        List<ProductSale> toRemove = sale.getProductSales().stream()
                .filter(ps -> ps.getProduct() != null)
                .filter(ps -> removeProductIds.contains(ps.getProduct().getProductId()))
                .collect(Collectors.toList());

        if (!toRemove.isEmpty()) {
            productSaleRepository.deleteAll(toRemove);
            sale.getProductSales().removeAll(toRemove);
        }
    }

    private boolean calculateActiveStatus(LocalDateTime stDate, LocalDateTime endDate) {
        LocalDateTime now = LocalDateTime.now();
        return !now.isBefore(stDate) && !now.isAfter(endDate);
    }

    private void validateSaleDates(LocalDateTime stDate, LocalDateTime endDate) {
        if (endDate.isBefore(stDate)) {
            throw new AppException(ErrorCode.INVALID_SALE_DATE);
        }
    }

    private void validateSaleValue(BigDecimal value) {
        if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(new BigDecimal("0.99")) > 0) {
            throw new AppException(ErrorCode.INVALID_SALE_VALUE);
        }
    }

    private BigDecimal getDiscountWithLock(Integer productId){

        String cacheKey = DISCOUNT_CACHE_PREFIX + productId;
        String lockKey = DISCOUNT_LOCK_PREFIX + productId;

        // Validate product không tồn tại
        String rawValue = redisService.getString(cacheKey);
        if(NULL_VALUE.equals(rawValue)){
            throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        // Lấy dữ liệu từ cache
        BigDecimal cacheValue = redisService.get(cacheKey, BigDecimal.class);
        if(cacheValue != null){
            return cacheValue;
        }

        RLock lock = redissonClient.getLock(lockKey);
        try{
            if(lock.tryLock(5, TimeUnit.SECONDS)){
                try{
                    // Double check
                    rawValue = redisService.getString(cacheKey);
                    if(NULL_VALUE.equals(rawValue)){
                        throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);
                    }

                    cacheValue = redisService.get(cacheKey, BigDecimal.class);
                    if(cacheValue != null){
                        return cacheValue;
                    }

                    log.info("Cache miss for Discount ID {}, reading from DB...", productId);

                    // Kiểm tra xem PRODUCT có sale không
                    if(!productRepository.existsById(productId)){
                        redisService.set(cacheKey, NULL_VALUE, Duration.ofMinutes(1));
                        throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);
                    }

                    LocalDateTime now = LocalDateTime.now();
                    List<ProductSale> activeProductSales = productSaleRepository
                            .findActiveSalesByProductIds(List.of(productId), now);

                    BigDecimal maxDiscount = activeProductSales.stream()
                            .map(ProductSale::getSaleValue)
                            .max(BigDecimal::compareTo)
                            .orElse(BigDecimal.ZERO);

                    redisService.set(cacheKey, maxDiscount, Duration.ofMinutes(30));

                    return maxDiscount;
                } finally {
                    if(lock.isHeldByCurrentThread()){
                        lock.unlock();
                    }
                }
            } else {
                throw new AppException(ErrorCode.SYSTEM_BUSY);
            }
        } catch (InterruptedException e){
            Thread.currentThread().interrupt();
            throw new AppException(ErrorCode.SYSTEM_ERROR);
        }

    }

    public void clearCache(List<Integer> productIds){

        if(productIds == null || productIds.isEmpty()) return;
        List<String> keys = productIds.stream()
                .distinct()
                .map(id -> DISCOUNT_CACHE_PREFIX + id)
                .toList();
        keys.forEach(redisService::delete);

    }

    private SalesResponse toResponse(Sales sales, boolean isActive) {
        List<ProductSaleItemResponse> productItems = (sales.getProductSales() == null || sales.getProductSales().isEmpty())
                ? List.of()  // TRẢ VỀ MẢNG RỖNG CHO SALE MỚI
                : sales.getProductSales().stream()
                .map(productSale -> {
                    if (productSale.getProduct() == null || productSale.getSaleValue() == null) {
                        return ProductSaleItemResponse.builder()
                                .id(0)
                                .value(BigDecimal.ZERO)
                                .image("")
                                .build();
                    }
                    return ProductSaleItemResponse.builder()
                            .id(productSale.getProduct().getProductId())
                            .value(productSale.getSaleValue())
                            .image(productSale.getProduct().getImage() != null
                                    ? productSale.getProduct().getImage()
                                    : "")
                            .build();
                })
                .collect(Collectors.toList());

        return SalesResponse.builder()
                .id(sales.getId())
                .name(sales.getName())
                .description(sales.getDescription() != null ? sales.getDescription() : "")
                .stDate(sales.getStDate())
                .endDate(sales.getEndDate())
                .active(isActive)
                .list_product(productItems)
                .build();
    }
}