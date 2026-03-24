package com.BTL_JAVA.BTL.Repository;

import com.BTL_JAVA.BTL.Entity.Orders.Order;
import com.BTL_JAVA.BTL.Entity.Payment;
import com.BTL_JAVA.BTL.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {

    // Tìm payment bằng VNPay transaction ref
    Optional<Payment> findByVnpayTransactionRef(String vnpayTransactionRef);

    // Tìm payment bằng order
    Optional<Payment> findByOrder(Order order);

    List<Payment> findByStatusAndPaymentMethodAndCreatedDateBefore(
            PaymentStatus status,
            String paymentMethod,
            LocalDateTime createdDate
    );
    
}
