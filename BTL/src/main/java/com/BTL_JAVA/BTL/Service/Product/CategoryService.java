package com.BTL_JAVA.BTL.Service.Product;

import com.BTL_JAVA.BTL.DTO.Request.Product.CategoryUpdateRequest;
import com.BTL_JAVA.BTL.DTO.Response.Product.ProductInCategoryResponse;
import com.BTL_JAVA.BTL.Entity.Product.Product;
import com.BTL_JAVA.BTL.DTO.Request.ApiResponse;
import com.BTL_JAVA.BTL.DTO.Request.Product.CategoryCreationRequest;
import com.BTL_JAVA.BTL.DTO.Response.Product.CategoryResponse;
import com.BTL_JAVA.BTL.Entity.Product.Category;
import com.BTL_JAVA.BTL.Entity.Product.ProductVariation;
import com.BTL_JAVA.BTL.Exception.AppException;
import com.BTL_JAVA.BTL.Exception.ErrorCode;
import com.BTL_JAVA.BTL.Repository.CategoryRepository;
import com.BTL_JAVA.BTL.Repository.ProductRepository;
import com.BTL_JAVA.BTL.Service.Cloudinary.UploadImageFile;
import com.BTL_JAVA.BTL.Service.RedisService;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CategoryService {

    CategoryRepository categoryRepository;
    UploadImageFile uploadImageFile;
    ProductRepository productRepository;

    SalesService salesCacheService;
    RedissonClient redissonClient;
    RedisService redisService;

    static String CATEGORY_DETAIL_CACHE = "category:detail:";
    static String CATEGORY_LIST_CACHE = "category:list:all";
    static String CATEGORY_LOCK = "lock:category:";
    static String LIST_LOCK = "lock:category:list";
    static String NULL_VALUE = "NOT_EXIST";

    public ApiResponse<CategoryResponse> create(CategoryCreationRequest request) throws IOException {
        Category cat=Category.builder()
                .name(request.getCategoryName())
                .parent_id(request.getPerentId() == null ? 0 : request.getPerentId())
                .build();
        //upload anh neu co

        if(request.getImage()!=null && !request.getImage().isEmpty()){
            var up=uploadImageFile.uploadImage(request.getImage());
            cat.setImageUrl(up);
            cat.setImagePublicId(up);
        }

        Category saved=categoryRepository.save(cat);

        Set<Integer> ids=(request.getProductIds()==null)?Set.of():request.getProductIds();
        if(!ids.isEmpty()){
            var prods=productRepository.findAllById(ids);
            Set<Integer> found = prods.stream().map(Product::getProductId).collect(Collectors.toSet());
            Set<Integer> missing = ids.stream().filter(i -> !found.contains(i)).collect(Collectors.toSet());
            if (!missing.isEmpty()) {
                throw new AppException(ErrorCode.PRODUCT_NOT_FOUND,"Product không tồn tại: " + missing);
            }
            // BÊN SỞ HỮU QUAN HỆ là Product -> setCategory
            prods.forEach(p -> p.setCategory(saved));
            productRepository.saveAll(prods);
        }

        clearListCache();

        return ApiResponse.ok(toResponse(saved, Map.of()));

    }

    public ApiResponse<Void> delete(Integer id) {

          Category cat = categoryRepository.findById(id)
                  .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

          // Không cho xoá khi còn sản phẩm
          if (cat.getProducts() != null && !cat.getProducts().isEmpty()) {
              throw new AppException(ErrorCode.PRODUCT_EXISTED);
          }

          categoryRepository.delete(cat);

          clearCategoryCache(id);
          clearListCache();

          return ApiResponse.ok(null);

    }

    public ApiResponse<CategoryResponse> get(Integer id) {

        String cacheKey = CATEGORY_DETAIL_CACHE + id;

        // Validate category không tồn tại
        String rawValue = redisService.getString(cacheKey);
        if(NULL_VALUE.equals(rawValue)){
            throw new AppException(ErrorCode.CATEGORY_NOT_FOUND);
        }

        // Lấy dữ liệu từ cache
        CategoryResponse cachedValue = redisService.get(cacheKey, CategoryResponse.class);
        if(cachedValue != null){
            return ApiResponse.ok(cachedValue);
        }

        // Cache miss
        String lockKey = CATEGORY_LOCK + id;
        RLock lock = redissonClient.getLock(lockKey);

        try{
            if(lock.tryLock(5, TimeUnit.SECONDS)){
                try{
                    // Double check
                    rawValue = redisService.getString(cacheKey);
                    if(NULL_VALUE.equals(rawValue)){
                        throw new AppException(ErrorCode.CATEGORY_NOT_FOUND);
                    }

                    cachedValue = redisService.get(cacheKey, CategoryResponse.class);
                    if(cachedValue != null){
                        return ApiResponse.ok(cachedValue);
                    }

                    log.info("Cache miss for Category ID {}, fetching from DB...", id);

                    Category category = categoryRepository.findById(id).orElse(null);
                    if(category == null){
                        redisService.set(cacheKey, NULL_VALUE, Duration.ofMinutes(1));
                        throw new AppException(ErrorCode.CATEGORY_NOT_FOUND);
                    }

                    List<Integer> productIds= category.getProducts().stream()
                            .map(Product::getProductId)
                            .toList();
                    Map<Integer, BigDecimal> discountMap = salesCacheService.getLastestDiscountMap(productIds);

                    CategoryResponse response = toResponse(category, discountMap);
                    redisService.set(cacheKey, response, Duration.ofMinutes(30));

                    return ApiResponse.ok(response);
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

    public ApiResponse<List<CategoryResponse>> list() {

        // Lấy dữ liệu từ cache
        List<CategoryResponse> cacheList = redisService.getList(CATEGORY_LIST_CACHE, new TypeReference<>(){});

        if(cacheList != null){
            return ApiResponse.ok(cacheList);
        }

        // Cache miss
        RLock lock = redissonClient.getLock(LIST_LOCK);
        try{
            if(lock.tryLock(5, TimeUnit.SECONDS)){
                try{
                    // Double check
                    cacheList = redisService.getList(CATEGORY_LIST_CACHE, new TypeReference<>(){});

                    if(cacheList != null){
                        return ApiResponse.ok(cacheList);
                    }

                    log.info("Cache miss for category list, reading from DB...");

                    // Tránh N + 1
                    List<Category> categories = categoryRepository.findAllWithProductsAndVariations();

                    List<Integer> allProductIds = categories.stream()
                            .flatMap(c -> c.getProducts().stream())
                            .map(Product::getProductId)
                            .distinct()
                            .toList();

                    Map<Integer, BigDecimal> discountMap = salesCacheService.getLastestDiscountMap(allProductIds);

                    List<CategoryResponse> data = categories.stream()
                            .map(c -> toResponse(c, discountMap))
                            .toList();

                    redisService.set(CATEGORY_LIST_CACHE, data, Duration.ofMinutes(30));

                    return ApiResponse.ok(data);
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

    public ApiResponse<CategoryResponse> update(Integer id, CategoryUpdateRequest request) throws IOException {
        Category cat = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        if (request.getCategoryName() != null) cat.setName(request.getCategoryName());
        if (request.getParentId() != null)     cat.setParent_id(request.getParentId());

        if (request.getImage() != null && !request.getImage().isEmpty()) {
            String url = uploadImageFile.uploadImage(request.getImage());
            cat.setImageUrl(url);
        }

        if (request.getAddProductIds() != null && !request.getAddProductIds().isEmpty()) {
            var prodsToAdd = productRepository.findAllById(request.getAddProductIds());
            var found = prodsToAdd.stream().map(Product::getProductId).collect(Collectors.toSet());
            var missing = request.getAddProductIds().stream().filter(id2 -> !found.contains(id2)).toList();
            if (!missing.isEmpty()) {
                throw new AppException(ErrorCode.PRODUCT_NOT_FOUND,"Product không tồn tại: " + missing);
            }
            prodsToAdd.forEach(p -> p.setCategory(cat));  // gán về category này
            productRepository.saveAll(prodsToAdd);
        }

        if (request.getRemoveProductIds() != null && !request.getRemoveProductIds().isEmpty()) {
            var prodsToRemove = productRepository.findAllById(request.getRemoveProductIds());
            // chỉ gỡ những product hiện đang thuộc category này
            prodsToRemove.stream()
                    .filter(p -> p.getCategory() != null && p.getCategory().getId() == cat.getId())
                    .forEach(p -> p.setCategory(null));
            productRepository.saveAll(prodsToRemove);
        }

        Category saved = categoryRepository.save(cat);

        clearCategoryCache(id);
        clearListCache();

        List<Integer> productIds = saved.getProducts().stream()
                .map(Product::getProductId)
                .toList();
        Map<Integer, BigDecimal> discountMap = salesCacheService.getLastestDiscountMap(productIds);

        return ApiResponse.ok(toResponse(saved, discountMap));
    }

    public void clearCategoryCache(Integer id){

        redisService.delete(CATEGORY_DETAIL_CACHE + id);
        log.info("Cleared detail cache for category {}", id);

    }

    public void clearListCache(){

        redisService.delete(CATEGORY_LIST_CACHE);
        log.info("Cleared category list cache");

    }

    private CategoryResponse toResponse(Category c, Map<Integer, BigDecimal> discountMap) {

        List<ProductInCategoryResponse> products = (c.getProducts() == null) ? List.of()
                : c.getProducts().stream().map(p -> {

            // Lấy danh sách ảnh từ các variation của product
            List<String> variationImages = (p.getProductVariations() == null) ? List.of()
                    : p.getProductVariations().stream()
                    .map(ProductVariation::getImage)
                    .filter(Objects::nonNull)
                    .toList();

            // Lấy sale active (nếu có) -> saleValue (tránh N + 1)
            BigDecimal saleValue = discountMap.get(p.getProductId());

            return ProductInCategoryResponse.builder()
                    .productId(p.getProductId())
                    .title(p.getTitle())
                    .price(p.getPrice())
                    .image(p.getImage())
                    .saleValue(saleValue)
                    .createdAt(p.getCreatedAt())
                    .variationCount(variationImages.size())
                    .variationImages(variationImages)
                    .build();
        }).toList();

        return CategoryResponse.builder()
                .categoryId(c.getId())
                .categoryName(c.getName())
                .parentId(c.getParent_id())
                .image(c.getImageUrl())
                .productCount(products.size())
                .products(products)
                .build();

    }
}
