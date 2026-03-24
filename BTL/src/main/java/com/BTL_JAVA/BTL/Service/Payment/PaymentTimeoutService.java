package com.BTL_JAVA.BTL.Service.Payment;

import com.BTL_JAVA.BTL.Entity.Payment;
import com.BTL_JAVA.BTL.Repository.PaymentRepository;
import com.BTL_JAVA.BTL.enums.PaymentStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class PaymentTimeoutService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Scheduled(fixedRate = 60000) // 1 phút chạy 1 lần
    public void checkExpiredPayments() {
        LocalDateTime fifteenMinutesAgo = LocalDateTime.now().minusMinutes(15);

        List<Payment> expiredPayments = paymentRepository
                .findByStatusAndPaymentMethodAndCreatedDateBefore(
                        PaymentStatus.PENDING,
                        "VNPAY",
                        fifteenMinutesAgo
                );

        for (Payment payment : expiredPayments) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
        }
    }
}
