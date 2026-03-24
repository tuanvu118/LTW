package com.BTL_JAVA.BTL.Entity;

import com.BTL_JAVA.BTL.Entity.Orders.Order;
import com.BTL_JAVA.BTL.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "payment_method", nullable = false)
    private String paymentMethod; // "VNPAY", "CASH"

    @Column(name = "amount", nullable = false)
    private Double amount;

    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "vnpay_transaction_ref")
    private String vnpayTransactionRef;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false,length = 20)
    private PaymentStatus status;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    @Column(columnDefinition = "TEXT")
    private String responseData;

    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now(); if (status == null) {
            status = PaymentStatus.PENDING;
        }
    }
}
