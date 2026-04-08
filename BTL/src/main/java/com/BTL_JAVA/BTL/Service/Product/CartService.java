package com.BTL_JAVA.BTL.Service.Product;

import com.BTL_JAVA.BTL.DTO.Request.ApiResponse;
import com.BTL_JAVA.BTL.DTO.Request.Product.CartItemRequest;
import com.BTL_JAVA.BTL.DTO.Request.Product.UpdateQuantityRequest;
import com.BTL_JAVA.BTL.DTO.Response.Product.CartItemResponse;
import com.BTL_JAVA.BTL.DTO.Response.Product.ProductVariationResponse;
import com.BTL_JAVA.BTL.Entity.Product.Cart;
import com.BTL_JAVA.BTL.Entity.Product.ProductVariation;
import com.BTL_JAVA.BTL.Entity.User;
import com.BTL_JAVA.BTL.Repository.CartRepository;
import com.BTL_JAVA.BTL.Repository.ProductVariationRepository;
import com.BTL_JAVA.BTL.Exception.AppException;
import com.BTL_JAVA.BTL.Exception.ErrorCode;
import com.BTL_JAVA.BTL.Repository.UserRepository;
import com.BTL_JAVA.BTL.Service.RedisService;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CartService {

    CartRepository cartRepository;
    ProductVariationRepository productVariationRepository;
    UserRepository userRepository;

    ProductVariationService productVariationCacheService;
    RedisService redisService;
    RedissonClient redissonClient;

    static String CART_CACHE_PREFIX = "cart:user:";
    static String CART_LOCK__PREFIX = "lock:cart:user:";

    public ApiResponse<List<CartItemResponse>> getCart() {

        Integer userId = getCurrentUserId();
        String cacheKey = CART_CACHE_PREFIX + userId;

        // Lấy dữ liệu từ cache
        List<CartItemResponse> cacheCart = redisService.getList(cacheKey, new TypeReference<>(){});
        if(cacheCart != null){
            return ApiResponse.<List<CartItemResponse>>builder()
                    .result(cacheCart)
                    .build();
        }

        // Cache miss
        String lockKey = CART_LOCK__PREFIX + userId;
        RLock lock = redissonClient.getLock(lockKey);

        try{
            if(lock.tryLock(5, TimeUnit.SECONDS)){
                try{
                    // Double check
                    cacheCart = redisService.getList(cacheKey, new TypeReference<>(){});
                    if(cacheCart != null){
                        return ApiResponse.<List<CartItemResponse>>builder()
                                .result(cacheCart)
                                .build();
                    }

                    log.info("Cache miss for Cart User ID {}, reading from DB...", userId);

                    List<Cart> cartItems = cartRepository.findByUserId(userId);
                    List<CartItemResponse> items = toCartItemResponseList(cartItems);

                    redisService.set(cacheKey, items, Duration.ofMinutes(15));
                    return ApiResponse.<List<CartItemResponse>>builder()
                            .result(items)
                            .build();
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

    @Transactional
    public ApiResponse<List<CartItemResponse>> addToCart(CartItemRequest request) {

        Integer userId = getCurrentUserId();

        String lockKey = CART_LOCK__PREFIX + userId;
        RLock lock = redissonClient.getLock(lockKey);

        try{
            if(lock.tryLock(5, TimeUnit.SECONDS)){
                try{

                    ProductVariationResponse variationDto = productVariationCacheService.get(request.getProduct_variation_id()).getResult();

                    Cart existingCart = cartRepository.findByUserIdAndProductVariationId(userId, variationDto.getId()).orElse(null);
                    int totalQuantity = request.getQuantity() + (existingCart != null ? existingCart.getQuantity() : 0);

                    // Kiểm tra tồn kho nhanh bằng dữ liệu DTO từ Cache
                    if (totalQuantity > variationDto.getStockQuantity()) {
                        throw new AppException(ErrorCode.NOT_ENOUGH_STOCK);
                    }

                    if (existingCart != null) {
                        existingCart.setQuantity(totalQuantity);
                        cartRepository.save(existingCart);
                    } else {
                        // TỐI ƯU: Dùng getReferenceById để lấy Entity Proxy, không tốn lệnh SELECT
                        ProductVariation variationEntity = productVariationRepository.getReferenceById(variationDto.getId());
                        User userEntity = userRepository.getReferenceById(userId);

                        cartRepository.save(Cart.builder()
                                .user(userEntity)
                                .productVariation(variationEntity)
                                .quantity(totalQuantity)
                                .build());
                    }

                    clearCache(userId);
                    return getCart();

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

    @Transactional
    public ApiResponse<List<CartItemResponse>> updateCartItem(Integer productVariationId, UpdateQuantityRequest request) {

        Integer userId = getCurrentUserId();

        String lockKey = CART_LOCK__PREFIX + userId;
        RLock lock = redissonClient.getLock(lockKey);

        try{
            if(lock.tryLock(5, TimeUnit.SECONDS)){
                try{
                    Cart existingCart = cartRepository.findByUserIdAndProductVariationId(userId, productVariationId)
                            .orElseThrow(() -> new AppException(ErrorCode.CART_NOT_FOUND));

                    if (request.getQuantity() == 0) {
                        cartRepository.delete(existingCart);
                    } else {
                        // Tận dụng Variation Cache để check stock
                        ProductVariationResponse variationDto = productVariationCacheService.get(productVariationId).getResult();
                        if (request.getQuantity() > variationDto.getStockQuantity()) {
                            throw new AppException(ErrorCode.NOT_ENOUGH_STOCK);
                        }
                        existingCart.setQuantity(request.getQuantity());
                        cartRepository.save(existingCart);
                    }

                    clearCache(userId);
                    return getCart();
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

    @Transactional
    public ApiResponse<List<CartItemResponse>> removeFromCart(Integer productVariationId) {

        Integer userId = getCurrentUserId();

        String lockKey = CART_LOCK__PREFIX + userId;
        RLock lock = redissonClient.getLock(lockKey);

        try{
            if(lock.tryLock(5, TimeUnit.SECONDS)){
                try{
                    cartRepository.findByUserIdAndProductVariationId(userId, productVariationId)
                            .ifPresent(cartRepository::delete);

                    clearCache(userId);
                    return getCart();
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

        String cacheKey = CART_CACHE_PREFIX + id;
        redisService.delete(cacheKey);
        log.info("Cleared cache for Cart User ID: {}", id);

    }

    private Integer getCurrentUserId() {

        var context = SecurityContextHolder.getContext();
        return Integer.parseInt(context.getAuthentication().getName());

    }

    private List<CartItemResponse> toCartItemResponseList(List<Cart> cartItems) {
        return cartItems.stream()
                .map(cart -> CartItemResponse.builder()
                        .cart_id(cart.getId())
                        .product_id(cart.getProductVariation().getProduct().getProductId())
                        .product_variation_id(cart.getProductVariation().getId())
                        .quantity(cart.getQuantity())
                        .build())
                .toList();
    }
}