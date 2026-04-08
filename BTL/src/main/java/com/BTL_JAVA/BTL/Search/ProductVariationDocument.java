package com.BTL_JAVA.BTL.Search;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductVariationDocument {
    @Field(type = FieldType.Integer)
    Integer id;

    @Field(type = FieldType.Keyword)
    String size;

    @Field(type = FieldType.Keyword)
    String color;

    @Field(type = FieldType.Integer)
    Integer stockQuantity;

    @Field(type = FieldType.Keyword)
    String image;
}
