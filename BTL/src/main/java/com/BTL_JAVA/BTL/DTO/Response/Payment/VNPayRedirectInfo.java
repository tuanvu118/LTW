package com.BTL_JAVA.BTL.DTO.Response.Payment;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VNPayRedirectInfo {

    boolean success;
    Integer orderId;

}
