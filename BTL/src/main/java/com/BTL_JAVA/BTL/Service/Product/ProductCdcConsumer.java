package com.BTL_JAVA.BTL.Service.Product;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductCdcConsumer {
    final ObjectMapper objectMapper;
    final ProductSearchIndexerService productSearchIndexerService;

    @Value("${app.cdc.topics.product}")
    String productTopic;

    @Value("${app.cdc.topics.product-variation}")
    String productVariationTopic;

    @Value("${app.cdc.topics.category}")
    String categoryTopic;

    @KafkaListener(
            id = "product-search-cdc-listener",
            topics = {
                    "${app.cdc.topics.product}",
                    "${app.cdc.topics.product-variation}",
                    "${app.cdc.topics.category}"
            }
    )
    public void onMessage(
            @Payload(required = false) String payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) throws IOException {

        if (payload == null || payload.isBlank()) {
            log.debug("Ignoring tombstone/empty message from topic {}", topic);
            return;
        }

        JsonNode root = objectMapper.readTree(payload);
        if (!root.has("op")) {
            log.debug("Ignoring non-DML Debezium message from topic {}", topic);
            return;
        }

        if (productTopic.equals(topic)) {
            handleProductChange(root);
            return;
        }
        if (productVariationTopic.equals(topic)) {
            handleVariationChange(root);
            return;
        }
        if (categoryTopic.equals(topic)) {
            handleCategoryChange(root);
        }
    }

    private void handleProductChange(JsonNode root) {
        String op = root.path("op").asText();
        Integer productId = readInteger(root.path("after"), "product_id");
        if ("d".equals(op)) {
            productId = readInteger(root.path("before"), "product_id");
            productSearchIndexerService.deleteProduct(productId);
            return;
        }

        productSearchIndexerService.reindexProduct(productId);
    }

    private void handleVariationChange(JsonNode root) {
        Set<Integer> impactedProductIds = new LinkedHashSet<>();
        impactedProductIds.add(readInteger(root.path("before"), "product_id"));
        impactedProductIds.add(readInteger(root.path("after"), "product_id"));

        impactedProductIds.stream()
                .filter(id -> id != null)
                .forEach(productSearchIndexerService::reindexProduct);
    }

    private void handleCategoryChange(JsonNode root) {
        Set<Integer> impactedCategoryIds = new LinkedHashSet<>();
        impactedCategoryIds.add(readInteger(root.path("before"), "category_id"));
        impactedCategoryIds.add(readInteger(root.path("after"), "category_id"));

        impactedCategoryIds.stream()
                .filter(id -> id != null)
                .forEach(productSearchIndexerService::reindexProductsByCategoryId);
    }

    private Integer readInteger(JsonNode node, String fieldName) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        JsonNode value = node.get(fieldName);
        if (value == null || value.isNull()) {
            return null;
        }
        return value.asInt();
    }
}
