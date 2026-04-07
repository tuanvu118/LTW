package com.BTL_JAVA.BTL.Service.Payment;

import com.BTL_JAVA.BTL.DTO.Response.Payment.PaymentResponse;
import com.BTL_JAVA.BTL.DTO.Response.Payment.VNPayApiResponse;
import com.BTL_JAVA.BTL.DTO.Response.Payment.VNPayPaymentResponse;
import com.BTL_JAVA.BTL.DTO.Response.Payment.VNPayRedirectInfo;
import com.BTL_JAVA.BTL.Entity.Orders.Order;
import com.BTL_JAVA.BTL.Entity.Orders.OrderDetail;
import com.BTL_JAVA.BTL.Entity.Payment;
import com.BTL_JAVA.BTL.Entity.Product.ProductVariation;
import com.BTL_JAVA.BTL.Exception.AppException;
import com.BTL_JAVA.BTL.Exception.ErrorCode;
import com.BTL_JAVA.BTL.Repository.OrderRepository;
import com.BTL_JAVA.BTL.Repository.PaymentRepository;
import com.BTL_JAVA.BTL.enums.OrderStatus;
import com.BTL_JAVA.BTL.enums.PaymentStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaymentService {

    PaymentRepository paymentRepository;
    OrderRepository orderRepository;
    VNPayService vnPayService;

    @Transactional
    public Object createPayment(Integer orderId, String paymentMethod, String bankCode, HttpServletRequest request){

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        Optional<Payment> existing = paymentRepository.findByOrder(order);
        if(existing.isPresent()){
            return mapToResponse(existing.get());
        }

        if(paymentMethod.equalsIgnoreCase("VNPAY")){
            return createVnPayPayment(order, bankCode, request);
        }

        if(paymentMethod.equalsIgnoreCase("CASH")){
            return createCashPayment(order);
        }

        throw new AppException(ErrorCode.INVALID_PAYMENT_METHOD);

    }

    private VNPayPaymentResponse createVnPayPayment(Order order, String backCode, HttpServletRequest request){

        VNPayPaymentResponse vnp = vnPayService.createPaymentUrl(order, backCode, request);

        Payment payment = Payment.builder()
                .order(order)
                .paymentMethod("VNPAY")
                .amount(order.getTotalAmount())
                .vnpayTransactionRef(vnp.getTransactionRef())
                .status(PaymentStatus.PENDING)
                .createdDate(LocalDateTime.now())
                .build();

        paymentRepository.save(payment);

        return vnp;

    }

    private PaymentResponse createCashPayment(Order order){

        Payment payment = Payment.builder()
                .order(order)
                .paymentMethod("CASH")
                .amount(order.getTotalAmount())
                .status(PaymentStatus.PENDING)
                .createdDate(LocalDateTime.now())
                .build();

        paymentRepository.save(payment);

        return mapToResponse(payment);

    }

    @Transactional
    public PaymentResponse confirmCashPayment(Integer orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        Payment payment = paymentRepository.findByOrder(order)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));

        if (!"CASH".equals(payment.getPaymentMethod())) {
            throw new AppException(ErrorCode.INVALID_PAYMENT_METHOD, "Phương thức thanh toán phải là CASH");
        }

        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setPaymentDate(LocalDateTime.now());

        order.setStatus(OrderStatus.APPROVED);

        return mapToResponse(payment);

    }

    @Transactional
    public PaymentResponse updatePaymentStatus(Integer paymentId, PaymentStatus status) {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));

        payment.setStatus(status);

        if (status == PaymentStatus.COMPLETED) {
            payment.setPaymentDate(LocalDateTime.now());
        }

        return mapToResponse(payment);

    }

    @Transactional
    public VNPayRedirectInfo handleVNPayReturn(Map<String, String> params){

        String responseCode = params.get("vnp_ResponseCode");
        String txnRef = params.get("vnp_TxnRef");
        String transactionNo = params.get("vnp_TransactionNo");

        Payment payment = paymentRepository.findByVnpayTransactionRef(txnRef)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));

        Order order = payment.getOrder();

        if(responseCode.equals("00")){
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setTransactionId(transactionNo);
            payment.setPaymentDate(LocalDateTime.now());
            payment.setResponseData(params.toString());

            order.setStatus(OrderStatus.APPROVED);

            return new VNPayRedirectInfo(true, order.getId());

//            return VNPayApiResponse.builder()
//                    .code("OK")
//                    .message("Thanh toán thành công! Đơn hàng #" + order.getId() + " đã được xác nhận.")
//                    .build();
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setResponseData(params.toString());

            cancelOrder(order);

            return new VNPayRedirectInfo(false, order.getId());

//            return VNPayApiResponse.builder()
//                    .code("NO")
//                    .message("\"Thanh toán thất bại! Đơn hàng #" + order.getId() + " đã bị huỷ. Mã lỗi: " + responseCode)
//                    .build();
        }

    }

    private void cancelOrder(Order order){

        for(OrderDetail detail : order.getOrderDetails()){
            ProductVariation variation = detail.getProductVariation();
            variation.setStockQuantity(variation.getStockQuantity() + detail.getQuantity());
        }

        order.setStatus(OrderStatus.CANCELED);

    }

    private PaymentResponse mapToResponse(Payment p) {

        return PaymentResponse.builder()
                .id(p.getId())
                .orderId(p.getOrder().getId())
                .paymentMethod(p.getPaymentMethod())
                .amount(p.getAmount())
                .status(p.getStatus().toString())
                .transactionId(p.getTransactionId())
                .createdDate(p.getCreatedDate())
                .paymentDate(p.getPaymentDate())
                .build();

    }

}