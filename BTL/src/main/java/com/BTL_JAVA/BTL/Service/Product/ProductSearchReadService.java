package com.BTL_JAVA.BTL.Service.Product;

import com.BTL_JAVA.BTL.DTO.Request.ApiResponse;
import com.BTL_JAVA.BTL.DTO.Response.PageResult;
import com.BTL_JAVA.BTL.DTO.Response.Product.ProductResponse;
import com.BTL_JAVA.BTL.DTO.Response.Product.ProductVariationResponse;
import com.BTL_JAVA.BTL.Search.ProductSearchDocument;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProductSearchReadService {
    ElasticsearchOperations elasticsearchOperations;
    ObjectMapper objectMapper;

    public ApiResponse<PageResult<ProductResponse>> search(
            String keyword,
            Double minPrice,
            Double maxPrice,
            List<String> sizes,
            List<String> colors,
            Pageable pageable) {

        Pageable effectivePageable = pageable;
        if (effectivePageable == null || effectivePageable.getPageSize() <= 0) {
            int pageNum = effectivePageable == null ? 0 : effectivePageable.getPageNumber();
            Sort sort = effectivePageable == null ? Sort.by(Sort.Order.asc("price")) : effectivePageable.getSort();
            effectivePageable = PageRequest.of(pageNum, 5, sort);
        }

        StringQuery query = new StringQuery(buildQuery(keyword, minPrice, maxPrice, sizes, colors));
        query.setPageable(effectivePageable);

        SearchHits<ProductSearchDocument> hits = elasticsearchOperations.search(query, ProductSearchDocument.class);

        List<ProductResponse> items = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(this::toResponse)
                .toList();

        PageResult<ProductResponse> payload = PageResult.<ProductResponse>builder()
                .items(items)
                .page(effectivePageable.getPageNumber())
                .size(effectivePageable.getPageSize())
                .total(hits.getTotalHits())
                .totalPages((int) Math.ceil((double) hits.getTotalHits() / effectivePageable.getPageSize()))
                .build();

        return ApiResponse.ok(payload);
    }

    private ProductResponse toResponse(ProductSearchDocument document) {
        List<ProductVariationResponse> variations = document.getVariations() == null
                ? List.of()
                : document.getVariations().stream()
                .map(variation -> ProductVariationResponse.builder()
                        .id(variation.getId())
                        .productId(document.getProductId())
                        .size(variation.getSize())
                        .color(variation.getColor())
                        .stockQuantity(variation.getStockQuantity())
                        .image(variation.getImage())
                        .build())
                .toList();

        return ProductResponse.builder()
                .productId(document.getProductId())
                .title(document.getTitle())
                .description(document.getDescription())
                .price(document.getPrice())
                .image(document.getImage())
                .categoryId(document.getCategoryId())
                .variationCount(document.getVariationCount())
                .createdAt(document.getCreatedAt())
                .variations(variations)
                .build();
    }

    private String buildQuery(
            String keyword,
            Double minPrice,
            Double maxPrice,
            List<String> sizes,
            List<String> colors) {

        ObjectNode root = objectMapper.createObjectNode();
        ObjectNode boolNode = objectMapper.createObjectNode();
        ArrayNode mustNode = objectMapper.createArrayNode();
        ArrayNode filterNode = objectMapper.createArrayNode();

        if (keyword != null && !keyword.isBlank()) {
            ObjectNode multiMatch = objectMapper.createObjectNode();
            multiMatch.put("query", keyword.trim());

            ArrayNode fields = objectMapper.createArrayNode();
            fields.add("title^3");
            fields.add("description");
            fields.add("categoryName");
            multiMatch.set("fields", fields);

            ObjectNode wrapper = objectMapper.createObjectNode();
            wrapper.set("multi_match", multiMatch);
            mustNode.add(wrapper);
        }

        if (minPrice != null || maxPrice != null) {
            ObjectNode rangeWrapper = objectMapper.createObjectNode();
            ObjectNode priceNode = objectMapper.createObjectNode();

            if (minPrice != null) {
                priceNode.put("gte", minPrice);
            }
            if (maxPrice != null) {
                priceNode.put("lte", maxPrice);
            }

            rangeWrapper.set("price", priceNode);
            ObjectNode filter = objectMapper.createObjectNode();
            filter.set("range", rangeWrapper);
            filterNode.add(filter);
        }

        addTermsFilter(filterNode, "sizes", sizes);
        addTermsFilter(filterNode, "colors", colors);

        if (mustNode.isEmpty() && filterNode.isEmpty()) {
            root.set("match_all", objectMapper.createObjectNode());
            return root.toString();
        }

        if (!mustNode.isEmpty()) {
            boolNode.set("must", mustNode);
        }
        if (!filterNode.isEmpty()) {
            boolNode.set("filter", filterNode);
        }

        root.set("bool", boolNode);
        return root.toString();
    }

    private void addTermsFilter(ArrayNode filterNode, String fieldName, List<String> values) {
        List<String> normalizedValues = values == null
                ? List.of()
                : values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(value -> value.toLowerCase(Locale.ROOT))
                .distinct()
                .toList();

        if (normalizedValues.isEmpty()) {
            return;
        }

        ObjectNode termsWrapper = objectMapper.createObjectNode();
        ArrayNode valuesNode = objectMapper.createArrayNode();
        normalizedValues.forEach(valuesNode::add);
        termsWrapper.set(fieldName, valuesNode);

        ObjectNode filter = objectMapper.createObjectNode();
        filter.set("terms", termsWrapper);
        filterNode.add(filter);
    }
}
