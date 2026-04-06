package com.BTL_JAVA.BTL.Service.Product;

import com.BTL_JAVA.BTL.DTO.Request.ApiResponse;
import com.BTL_JAVA.BTL.DTO.Request.Sales.SalesCreationRequest;
import com.BTL_JAVA.BTL.DTO.Request.Sales.SalesUpdateRequest;
import com.BTL_JAVA.BTL.DTO.Request.Sales.ProductSaleItemRequest;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    RedisTemplate<String, Object> redisTemplate;
    RedissonClient redissonClient;

    static String DISCOUNT_CACHE_PREFIX = "product:discount:";
    static String DISCOUNT_LOCK_PREFIX = "lock:discount:";
    static String NULL_VALUE = "NOT_EXIST";

    public ApiResponse<List<SalesResponse>> getAllSales(Boolean active) {
        try {
            List<Sales> allSales = salesRepository.findAll();

            List<SalesResponse> salesResponses = allSales.stream()
                    .map(sale -> {
                        boolean isActive = calculateActiveStatus(sale.getStDate(), sale.getEndDate());
                        return toResponse(sale, isActive);
                    })
                    .collect(Collectors.toList());

            if (active != null) {
                salesResponses = salesResponses.stream()
                        .filter(sale -> sale.getActive().equals(active))
                        .collect(Collectors.toList());
            }

            return ApiResponse.<List<SalesResponse>>builder()
                    .result(salesResponses)
                    .build();
        } catch (Exception e) {
            throw new AppException(ErrorCode.SALES_NOT_FOUND);
        }
    }

    public Map<Integer, BigDecimal> getLastestDiscountMap(List<Integer> productIds){

        Map<Integer, BigDecimal> resultMap = new HashMap<>();
        if(productIds == null || productIds.isEmpty()) return resultMap;

        // Lấy dữ liệu từ cache
        List<String> keys = productIds.stream()
                .map(id -> DISCOUNT_CACHE_PREFIX + id)
                .toList();
        List<Object> cacheValues = redisTemplate.opsForValue().multiGet(keys);

        List<Integer> missIds = new ArrayList<>();

        // Validate variation không tồn tại
        for(int i = 0; i < productIds.size(); i++){
            Integer productId = productIds.get(i);
            Object cacheValue = cacheValues != null ? cacheValues.get(i) : null;

            if(cacheValue != null){
                if(cacheValue.equals(NULL_VALUE)){
                    log.warn("Product ID {} marked as NOT EXIST in cache", productId);
                    continue;
                }
                resultMap.put(productId, new BigDecimal(cacheValue.toString()));
            }
            else{
                missIds.add(productId);
            }
        }

        // Cache miss
        if(!missIds.isEmpty()){
            for(Integer pId : missIds){
                try{
                    BigDecimal discount = getDiscountWithLock(pId);
                    resultMap.put(pId, discount);
                } catch (AppException e){
                    if(e.getErrorCode() == ErrorCode.PRODUCT_NOT_FOUND){
                        continue; //Bỏ qua sản phẩm ảo
                    }
                    throw e;
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
        Object cacheValue = redisTemplate.opsForValue().get(cacheKey);
        if (cacheValue != null) {
            if (cacheValue.equals(NULL_VALUE)){
                throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);
            }
            return new BigDecimal(cacheValue.toString());
        }

        RLock lock = redissonClient.getLock(lockKey);
        try{
            if(lock.tryLock(5, TimeUnit.SECONDS)){
                try{
                    // Double check
                    cacheValue = redisTemplate.opsForValue().get(cacheKey);
                    if(cacheValue != null){
                        if(cacheValue.equals(NULL_VALUE)){
                            throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);
                        }
                        return new BigDecimal(cacheValue.toString());
                    }

                    log.info("Cache miss for Discount ID {}, reading from DB...", productId);

                    // Kiểm tra xem PRODUCT có sale không
                    if(!productRepository.existsById(productId)){
                        redisTemplate.opsForValue().set(cacheKey, NULL_VALUE, Duration.ofMinutes(1));
                        throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);
                    }

                    LocalDateTime now = LocalDateTime.now();
                    List<ProductSale> activeProductSales = productSaleRepository
                            .findActiveSalesByProductIds(List.of(productId), now);

                    BigDecimal maxDiscount = activeProductSales.stream()
                            .map(ProductSale::getSaleValue)
                            .max(BigDecimal::compareTo)
                            .orElse(BigDecimal.ZERO);

                    redisTemplate.opsForValue().set(cacheKey, maxDiscount, Duration.ofMinutes(30));

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
                .map(id -> DISCOUNT_CACHE_PREFIX + id)
                .toList();
        redisTemplate.delete(keys);

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