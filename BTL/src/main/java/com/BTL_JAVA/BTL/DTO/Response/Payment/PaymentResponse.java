package com.BTL_JAVA.BTL.DTO.Response.Payment;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentResponse {
    Integer id;
    Integer orderId;
    String paymentMethod;
    Double amount;
    String status;
    String transactionId;
    LocalDateTime createdDate;
    LocalDateTime paymentDate;
}
