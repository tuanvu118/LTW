package com.BTL_JAVA.BTL.Service.Product;

import com.BTL_JAVA.BTL.DTO.Request.ApiResponse;
import com.BTL_JAVA.BTL.DTO.Request.Product.ProductCreationRequest;
import com.BTL_JAVA.BTL.DTO.Request.Product.ProductUpdateRequest;
import com.BTL_JAVA.BTL.DTO.Response.PageResult;
import com.BTL_JAVA.BTL.DTO.Response.Product.*;
import com.BTL_JAVA.BTL.Entity.Product.Category;
import com.BTL_JAVA.BTL.Entity.Product.Product;
import com.BTL_JAVA.BTL.Entity.Product.ProductVariation;
import com.BTL_JAVA.BTL.Exception.AppException;
import com.BTL_JAVA.BTL.Exception.ErrorCode;
import com.BTL_JAVA.BTL.Repository.CategoryRepository;
import com.BTL_JAVA.BTL.Repository.ProductRepository;
import com.BTL_JAVA.BTL.Repository.ProductSaleRepository;
import com.BTL_JAVA.BTL.Repository.ProductVariationRepository;
import com.BTL_JAVA.BTL.Service.Cloudinary.UploadImageFile;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProductService {

    ProductRepository productRepository;
    CategoryRepository categoryRepository;
    ProductVariationRepository productVariationRepository;
    UploadImageFile uploadImageFile;
    ProductSaleRepository productSaleRepository;
    ProductSearchReadService productSearchReadService;

    @Transactional
    public ApiResponse<ProductResponse> create(ProductCreationRequest req) throws IOException {
        Product p = Product.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .price(req.getPrice())
                .build();

        if (req.getCategoryId() != null) {
            Category cat = categoryRepository.findById(req.getCategoryId())
                    .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
            p.setCategory(cat);
        }

        if (req.getImage() != null && !req.getImage().isEmpty()) {
            String url = uploadImageFile.uploadImage(req.getImage());
            p.setImage(url);
        }

        Product saved = productRepository.save(p);

        Set<Integer> vIds = req.getVariationIds() == null ? Set.of() : req.getVariationIds();
        if (!vIds.isEmpty()) {
            var variations = productVariationRepository.findAllById(vIds);
            var found = variations.stream().map(ProductVariation::getId).collect(Collectors.toSet());
            var missing = vIds.stream().filter(i -> !found.contains(i)).collect(Collectors.toSet());
            if (!missing.isEmpty()) {
                throw new AppException(ErrorCode.VARIATION_NOT_FOUND, "Variation khong ton tai: " + missing);
            }
            variations.forEach(v -> v.setProduct(saved));
            productVariationRepository.saveAll(variations);
        }

        return ApiResponse.<ProductResponse>builder()
                .code(1000).message("Success")
                .result(toResponse(saved))
                .build();
    }


    @Transactional
    public ApiResponse<ProductResponse> update(Integer id, ProductUpdateRequest req) throws IOException {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        if (req.getTitle() != null) p.setTitle(req.getTitle());
        if (req.getDescription() != null) p.setDescription(req.getDescription());
        if (req.getPrice() != null) p.setPrice(req.getPrice());

        if (req.getCategoryId() != null) {
            Category cat = categoryRepository.findById(req.getCategoryId())
                    .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
            p.setCategory(cat);
        }

        if (req.getImage() != null && !req.getImage().isEmpty()) {
            String url = uploadImageFile.uploadImage(req.getImage());
            p.setImage(url);
        }


        if (req.getAddVariationIds() != null && !req.getAddVariationIds().isEmpty()) {
            var toAdd = productVariationRepository.findAllById(req.getAddVariationIds());
            var found = toAdd.stream().map(ProductVariation::getId).collect(Collectors.toSet());
            var missing = req.getAddVariationIds().stream().filter(i -> !found.contains(i)).toList();
            if (!missing.isEmpty()) {
                throw new AppException(ErrorCode.VARIATION_NOT_FOUND, "Variation khong ton tai: " + missing);
            }
            toAdd.forEach(v -> v.setProduct(p));
            productVariationRepository.saveAll(toAdd);
        }

        if (req.getRemoveVariationIds() != null && !req.getRemoveVariationIds().isEmpty()) {
            var toRemove = productVariationRepository.findAllById(req.getRemoveVariationIds());
            toRemove.stream()
                    .filter(v -> v.getProduct() != null && v.getProduct().getProductId() == p.getProductId())
                    .forEach(v -> v.setProduct(null));
            productVariationRepository.saveAll(toRemove);
        }

        Product saved = productRepository.save(p);

        return ApiResponse.<ProductResponse>builder()
                .code(1000).message("Success")
                .result(toResponse(saved))
                .build();
    }

    @Transactional
    public ApiResponse<Void> delete(Integer id) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> new AppException((ErrorCode.PRODUCT_NOT_FOUND)));


        if (p.getProductVariations() != null && !p.getProductVariations().isEmpty()) {
            throw new AppException(ErrorCode.VARIATION_EXISTED);
        }
        var productSales = productSaleRepository.findByProduct(p);
        if (!productSales.isEmpty()) {
            productSaleRepository.deleteAll(productSales);
        }

        productRepository.delete(p);
        return ApiResponse.<Void>builder()
                .code(1000).message("Deleted").result(null).build();
    }


    public ApiResponse<ProductDetailResponse> get(Integer id) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> new AppException((ErrorCode.PRODUCT_NOT_FOUND)));

        return ApiResponse.<ProductDetailResponse>builder()
                .code(1000).message("Success")
                .result(toDetailResponse(p))
                .build();
    }


    public ApiResponse<List<ProductResponse>> list() {
        var all = productRepository.findAll();

        var data = all.stream()
                .map(this::toResponse)
                .toList();

        return ApiResponse.<List<ProductResponse>>builder()
                .code(1000).message("Success")
                .result(data)
                .build();
    }

    private ProductResponse toResponse(Product p) {
        var variations = (p.getProductVariations() == null) ? List.<ProductVariationResponse>of()
                : p.getProductVariations().stream()
                .map(v -> ProductVariationResponse.builder()
                        .id(v.getId())
                        .productId(p.getProductId())
                        .image(v.getImage())
                        .size(v.getSize())
                        .color(v.getColor())
                        .stockQuantity(v.getStockQuantity())
                        .build())
                .toList();

        return ProductResponse.builder()
                .productId(p.getProductId())
                .title(p.getTitle())
                .description(p.getDescription())
                .price(p.getPrice())
                .image(p.getImage())
                .categoryId(p.getCategory() == null ? null : p.getCategory().getId())
                .variationCount(variations.size())
                .createdAt(p.getCreatedAt())
                .variations(variations)
                .build();
    }

    private ProductDetailResponse toDetailResponse(Product p) {
        Map<String, List<ProductVariation>> byColor = new java.util.LinkedHashMap<>();
        if (p.getProductVariations() != null) {
            for (var v : p.getProductVariations()) {
                String color = v.getColor();
                byColor.computeIfAbsent(color, k -> new java.util.ArrayList<>()).add(v);
            }
        }

        var groups = byColor.entrySet().stream().map(e -> {
            String color = e.getKey();
            var list = e.getValue();

            String image = list.stream()
                    .map(ProductVariation::getImage)
                    .filter(java.util.Objects::nonNull)
                    .findFirst()
                    .orElse(null);

            var sizes = list.stream()
                    .map(v -> SizeItem.builder()
                            .idVariation(v.getId())
                            .size(v.getSize())
                            .stockQuantity(v.getStockQuantity() == null ? 0 : v.getStockQuantity())
                            .build())
                    .toList();

            return ProductVariationGroup.builder()
                    .productId(p.getProductId())
                    .image(image)
                    .color(color)
                    .list(sizes)
                    .build();
        }).toList();

        BigDecimal discount = null;
        var now = java.time.LocalDateTime.now();
        var ps = productSaleRepository.findActiveProductSaleByProductId(p.getProductId(), now)
                .stream().findFirst().orElse(null);
        if (ps != null) discount = ps.getSaleValue();

        return ProductDetailResponse.builder()
                .productId(p.getProductId())
                .title(p.getTitle())
                .description(p.getDescription())
                .price(p.getPrice())
                .image(p.getImage())
                .saleValue(discount)
                .createdAt(p.getCreatedAt())
                .listVariations(groups)
                .build();
    }


    public ApiResponse<PageResult<ProductResponse>> search(
            String keyword,
            Double minPrice,
            Double maxPrice,
            List<String> sizes,
            List<String> colors,
            Pageable pageable) {

        if (pageable == null || pageable.getPageSize() <= 0) {
            int pageNum = (pageable == null) ? 0 : pageable.getPageNumber();
            var sort = (pageable == null) ? Sort.unsorted() : pageable.getSort();
            pageable = PageRequest.of(pageNum, 5, sort);
        }

        return productSearchReadService.search(keyword, minPrice, maxPrice, sizes, colors, pageable);
    }
}
