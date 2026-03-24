package com.BTL_JAVA.BTL.Service.Payment;

import com.BTL_JAVA.BTL.DTO.Response.Payment.PaymentResponse;
import com.BTL_JAVA.BTL.Entity.Payment;
import com.BTL_JAVA.BTL.Exception.AppException;
import com.BTL_JAVA.BTL.Exception.ErrorCode;
import com.BTL_JAVA.BTL.Repository.PaymentRepository;
import com.BTL_JAVA.BTL.enums.PaymentStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    public PaymentResponse updatePaymentStatus(Integer paymentId, PaymentStatus status) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));

        payment.setStatus(status);
        Payment updatedPayment = paymentRepository.save(payment);

        return PaymentResponse.builder()
                .id(updatedPayment.getId())
                .orderId(updatedPayment.getOrder().getId())
                .paymentMethod(updatedPayment.getPaymentMethod())
                .amount(updatedPayment.getAmount())
                .status(updatedPayment.getStatus().toString())
                .transactionId(updatedPayment.getTransactionId())
                .createdDate(updatedPayment.getCreatedDate())
                .paymentDate(updatedPayment.getPaymentDate())
                .build();
    }
}