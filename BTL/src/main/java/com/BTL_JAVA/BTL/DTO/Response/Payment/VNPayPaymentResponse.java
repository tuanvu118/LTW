package com.BTL_JAVA.BTL.DTO.Response.Payment;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VNPayPaymentResponse {
    String code;
    String message;
    String paymentUrl;
    String transactionRef;
}