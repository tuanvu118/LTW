package com.BTL_JAVA.BTL.DTO.Response.Product;


import lombok.*;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductVariationResponse implements Serializable {
        Integer id;
        Integer productId;
        String  size;
        String  color;
        Integer stockQuantity;
        String  image;
}

