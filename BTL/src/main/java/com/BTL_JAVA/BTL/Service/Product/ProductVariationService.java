package com.BTL_JAVA.BTL.Service.Product;

import com.BTL_JAVA.BTL.DTO.Request.ApiResponse;
import com.BTL_JAVA.BTL.DTO.Request.Product.ProductVariationCreationRequest;
import com.BTL_JAVA.BTL.DTO.Request.Product.ProductVariationUpdateRequest;
import com.BTL_JAVA.BTL.DTO.Response.Product.ProductVariationResponse;
import com.BTL_JAVA.BTL.Entity.Product.Product;
import com.BTL_JAVA.BTL.Entity.Product.ProductVariation;
import com.BTL_JAVA.BTL.Exception.AppException;
import com.BTL_JAVA.BTL.Exception.ErrorCode;
import com.BTL_JAVA.BTL.Repository.ProductRepository;
import com.BTL_JAVA.BTL.Repository.ProductVariationRepository;
import com.BTL_JAVA.BTL.Service.Cloudinary.UploadImageFile;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProductVariationService {

    ProductVariationRepository productVariationRepository;
    ProductRepository productRepository;
    UploadImageFile uploadImageFile;

    RedisTemplate<String, Object> redisTemplate;
    RedissonClient redissonClient;

    static String CACHE_KEY_PREFIX = "variation:";
    static String LOCK_KEY_PREFIX = "lock:variation:";
    static String NULL_VALUE = "NOT_EXIST";

    public ApiResponse<ProductVariationResponse> create(ProductVariationCreationRequest request) throws IOException {

        if (request.getProductId() == null||request.getSize()==null||request.getColor()==null) {
            throw new AppException(ErrorCode.INVALID_VARIATION);
        }

        if(productVariationRepository.existsByProduct_ProductIdAndSizeIgnoreCaseAndColorIgnoreCase(request.getProductId(), request.getSize(), request.getColor())) {
            throw new AppException(ErrorCode.DUPLICATE_VARIATION);
        }

        Product productRef = productRepository.getReferenceById(request.getProductId());

        ProductVariation pv = ProductVariation.builder()
                .product(productRef)
                .size(request.getSize())
                .color(request.getColor())
                .stockQuantity(request.getStockQuantity() == null ? 0 : request.getStockQuantity())
                .build();

        if (request.getImage() != null && !request.getImage().isEmpty()) {
            String url = uploadImageFile.uploadImage(request.getImage());
            pv.setImage(url);
        }

        ProductVariation saved = productVariationRepository.save(pv);

        clearCache(saved.getId());

        return ApiResponse.ok(toResponse(saved));

    }

    // UPDATE (partial + có thể chuyển sang product khác)
    @Transactional
    public ApiResponse<ProductVariationResponse> update(Integer id, ProductVariationUpdateRequest request) throws IOException {

        ProductVariation pv = productVariationRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.VARIATION_NOT_FOUND));

        Integer productId = request.getProductId() != null ? request.getProductId()
                : (pv.getProduct() != null ? pv.getProduct().getProductId() : null);

        String size  = request.getSize()  != null ? request.getSize().trim()  : pv.getSize();
        String color = request.getColor() != null ? request.getColor().trim() : pv.getColor();

        if (productVariationRepository
                .existsByProduct_ProductIdAndSizeIgnoreCaseAndColorIgnoreCaseAndIdNot(productId, size, color, id)) {
            throw new AppException(ErrorCode.DUPLICATE_VARIATION);
        }

        if (request.getProductId() != null) {
            Product productRef = productRepository.getReferenceById(request.getProductId());
            pv.setProduct(productRef);
        }

        if (request.getSize() != null)          pv.setSize(request.getSize());
        if (request.getColor() != null)         pv.setColor(request.getColor());
        if (request.getStockQuantity() != null) pv.setStockQuantity(request.getStockQuantity());

        if (request.getImage() != null && !request.getImage().isEmpty()) {
            String url = uploadImageFile.uploadImage(request.getImage());
            pv.setImage(url);
        }

        ProductVariation saved = productVariationRepository.save(pv);

        clearCache(id);

        return ApiResponse.ok(toResponse(saved));

    }

    @Transactional
    public ApiResponse<Void> delete(Integer id) {

        ProductVariation pv = productVariationRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.VARIATION_NOT_FOUND));
        productVariationRepository.delete(pv);
        clearCache(id);
        return ApiResponse.ok(null);

    }

    public ApiResponse<ProductVariationResponse> get(Integer id) {

        String cacheKey = CACHE_KEY_PREFIX + id;

        // Lấy dữ liệu từ cache
        Object cachedValue = redisTemplate.opsForValue().get(cacheKey);

        // Validate variation không tồn tại
        if(cachedValue != null){
            if(cachedValue.equals(NULL_VALUE)){
                throw new AppException(ErrorCode.VARIATION_NOT_FOUND);
            }
            return ApiResponse.ok((ProductVariationResponse) cachedValue);
        }

        // Cache miss
        String lockKey = LOCK_KEY_PREFIX + id;
        RLock lock = redissonClient.getLock(lockKey);

        try{
            if(lock.tryLock(5, TimeUnit.SECONDS)){
                try{
                    // Double check
                    cachedValue = redisTemplate.opsForValue().get(cacheKey);
                    if(cachedValue != null){
                        if(cachedValue.equals(NULL_VALUE)){
                            throw new AppException(ErrorCode.VARIATION_NOT_FOUND);
                        }
                        return ApiResponse.ok((ProductVariationResponse) cachedValue);
                    }

                    log.info("Cache miss for ID {}, reading from DB...", id);
                    ProductVariation pv = productVariationRepository.findById(id)
                            .orElse(null);
                    if(pv == null){
                        redisTemplate.opsForValue().set(cacheKey, NULL_VALUE, Duration.ofMinutes(1));
                        throw new AppException(ErrorCode.VARIATION_NOT_FOUND);
                    }

                    ProductVariationResponse response = toResponse(pv);
                    redisTemplate.opsForValue().set(cacheKey, response, Duration.ofMinutes(30));

                    return ApiResponse.ok(response);
                } finally {
                    if(lock.isHeldByCurrentThread()){
                        lock.unlock();
                    }
                }
            } else{
                throw new AppException(ErrorCode.SYSTEM_BUSY);
            }
        } catch (InterruptedException e){
            Thread.currentThread().interrupt();
            throw new AppException(ErrorCode.SYSTEM_ERROR);
        }

    }

    public void clearCache(Integer id){
        String cacheKey = CACHE_KEY_PREFIX + id;
        redisTemplate.delete(cacheKey);
        log.info("Cleared cache for Variation ID: {}", id);
    }


    public ApiResponse<List<ProductVariationResponse>> list() {
        var all = productVariationRepository.findAll();
        var data = all.stream().map(this::toResponse).toList();
        return ApiResponse.ok(data);
    }


    private ProductVariationResponse toResponse(ProductVariation pv) {
        return ProductVariationResponse.builder()
                .id(pv.getId())
                .productId(pv.getProduct() == null ? null : pv.getProduct().getProductId())
                .size(pv.getSize())
                .color(pv.getColor())
                .stockQuantity(pv.getStockQuantity())
                .image(pv.getImage())
                .build();
    }

}

