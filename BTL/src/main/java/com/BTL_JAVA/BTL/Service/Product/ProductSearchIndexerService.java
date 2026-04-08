package com.BTL_JAVA.BTL.Service.Product;

import com.BTL_JAVA.BTL.Entity.Product.Product;
import com.BTL_JAVA.BTL.Repository.ProductRepository;
import com.BTL_JAVA.BTL.Repository.ProductSearchRepository;
import com.BTL_JAVA.BTL.Search.ProductSearchDocument;
import com.BTL_JAVA.BTL.Search.ProductVariationDocument;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProductSearchIndexerService {
    ProductRepository productRepository;
    ProductSearchRepository productSearchRepository;

    @Transactional(readOnly = true)
    public void reindexProduct(Integer productId) {
        if (productId == null) {
            return;
        }

        Optional<ProductSearchDocument> document = productRepository.findWithCategoryAndVariationsByProductId(productId)
                .map(this::toDocument);

        if (document.isPresent()) {
            productSearchRepository.save(document.get());
            log.info("Indexed product {} into Elasticsearch", productId);
            return;
        }

        productSearchRepository.deleteById(productId);
        log.info("Deleted product {} from Elasticsearch because source row no longer exists", productId);
    }

    public void deleteProduct(Integer productId) {
        if (productId == null) {
            return;
        }

        productSearchRepository.deleteById(productId);
        log.info("Deleted product {} from Elasticsearch", productId);
    }

    @Transactional(readOnly = true)
    public void reindexProductsByCategoryId(Integer categoryId) {
        if (categoryId == null) {
            return;
        }

        productRepository.findIdsByCategoryId(categoryId).forEach(this::reindexProduct);
    }

    private ProductSearchDocument toDocument(Product product) {
        List<ProductVariationDocument> variations = product.getProductVariations() == null
                ? List.of()
                : product.getProductVariations().stream()
                .sorted(Comparator.comparingInt(v -> v.getId()))
                .map(variation -> ProductVariationDocument.builder()
                        .id(variation.getId())
                        .size(variation.getSize())
                        .color(variation.getColor())
                        .stockQuantity(variation.getStockQuantity())
                        .image(variation.getImage())
                        .build())
                .toList();

        List<String> sizes = variations.stream()
                .map(ProductVariationDocument::getSize)
                .filter(Objects::nonNull)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .distinct()
                .toList();

        List<String> colors = variations.stream()
                .map(ProductVariationDocument::getColor)
                .filter(Objects::nonNull)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .distinct()
                .toList();

        return ProductSearchDocument.builder()
                .productId(product.getProductId())
                .title(product.getTitle())
                .description(product.getDescription())
                .price(product.getPrice())
                .image(product.getImage())
                .categoryId(product.getCategory() == null ? null : product.getCategory().getId())
                .categoryName(product.getCategory() == null ? null : product.getCategory().getName())
                .variationCount(variations.size())
                .sizes(sizes)
                .colors(colors)
                .variations(variations)
                .createdAt(product.getCreatedAt())
                .build();
    }
}
