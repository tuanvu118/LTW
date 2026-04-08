package com.BTL_JAVA.BTL.Search;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Document(indexName = "products_v1", createIndex = true)
public class ProductSearchDocument {
    @Id
    Integer productId;

    @Field(type = FieldType.Text)
    String title;

    @Field(type = FieldType.Text)
    String description;

    @Field(type = FieldType.Double)
    Double price;

    @Field(type = FieldType.Keyword)
    String image;

    @Field(type = FieldType.Integer)
    Integer categoryId;

    @Field(type = FieldType.Text)
    String categoryName;

    @Field(type = FieldType.Integer)
    Integer variationCount;

    @Field(type = FieldType.Keyword)
    List<String> sizes;

    @Field(type = FieldType.Keyword)
    List<String> colors;

    @Field(type = FieldType.Nested)
    List<ProductVariationDocument> variations;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_fraction)
    LocalDateTime createdAt;
}
