package com.BTL_JAVA.BTL.Controller.Payment;
import com.BTL_JAVA.BTL.DTO.Response.Payment.VNPayApiResponse;
import com.BTL_JAVA.BTL.DTO.Response.Payment.VNPayPaymentResponse;
import com.BTL_JAVA.BTL.DTO.Response.Payment.PaymentResponse;
import com.BTL_JAVA.BTL.Entity.Orders.Order;
import com.BTL_JAVA.BTL.Entity.Orders.OrderDetail;
import com.BTL_JAVA.BTL.Entity.Payment;
import com.BTL_JAVA.BTL.Entity.Product.ProductVariation;
import com.BTL_JAVA.BTL.Repository.OrderRepository;
import com.BTL_JAVA.BTL.Repository.PaymentRepository;
import com.BTL_JAVA.BTL.Service.Payment.PaymentService;
import com.BTL_JAVA.BTL.configuration.VNPayConfig;
import com.BTL_JAVA.BTL.enums.OrderStatus;
import com.BTL_JAVA.BTL.enums.PaymentStatus;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping ("/api/payment")
public class PaymentController {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentService paymentService;

    @PreAuthorize("isAuthenticated()")
    @Transactional
    @PostMapping("/create")
    public ResponseEntity<?> createPayment(
            @RequestParam("orderId") Integer orderId,
            @RequestParam("paymentMethod") String paymentMethod, // "VNPAY" hoặc "CASH"
            @RequestParam(value = "bankCode", defaultValue = "NCB") String bankCode,
            HttpServletRequest request) {

        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

            // Kiểm tra nếu order đã có payment
            Optional<Payment> existingPayment = paymentRepository.findByOrder(order);
            if (existingPayment.isPresent()) {
                Payment payment = existingPayment.get();
                PaymentResponse response = PaymentResponse.builder()
                        .id(payment.getId())
                        .orderId(order.getId())
                        .paymentMethod(payment.getPaymentMethod())
                        .amount(payment.getAmount())
                        .status(payment.getStatus().toString())
                        .build();
                return ResponseEntity.ok(response);
            }

            // Xử lý theo payment method
            if ("VNPAY".equalsIgnoreCase(paymentMethod)) {
                return handleVNPayPayment(order, bankCode, request);
            } else if ("CASH".equalsIgnoreCase(paymentMethod)) {
                return handleCashPayment(order);
            } else {
                return ResponseEntity.badRequest().body(
                        Map.of("error", "Invalid payment method")
                );
            }


        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Payment creation failed: " + e.getMessage())
            );
        }
    }

    // XỬ LÝ VNPAY PAYMENT
    private ResponseEntity<?> handleVNPayPayment(Order order, String bankCode, HttpServletRequest request)
            throws UnsupportedEncodingException {

        Double amount = order.getTotalAmount();
        long vnp_Amount = (long) (amount * 100);
        String vnp_TxnRef = VNPayConfig.getRandomNumber(8);
        String vnp_IpAddr = VNPayConfig.getIpAddress(request); // Sửa IP thực

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", VNPayConfig.vnp_Version);
        vnp_Params.put("vnp_Command", VNPayConfig.vnp_Command);
        vnp_Params.put("vnp_TmnCode", VNPayConfig.vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(vnp_Amount));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_BankCode", bankCode);
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", "Thanh toan don hang:" + order.getId() + " Ma: " + vnp_TxnRef);
        vnp_Params.put("vnp_OrderType", "other");
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", VNPayConfig.vnp_ReturnUrl);
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        // Build query URL
        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();

        for (Iterator<String> itr = fieldNames.iterator(); itr.hasNext();) {
            String fieldName = itr.next();
            String fieldValue = vnp_Params.get(fieldName);

            if (fieldValue != null && fieldValue.length() > 0) {
                hashData.append(fieldName).append('=')
                        .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()))
                        .append('=')
                        .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));

                if (itr.hasNext()) {
                    hashData.append('&');
                    query.append('&');
                }
            }
        }

        String vnp_SecureHash = VNPayConfig.hmacSHA512(VNPayConfig.secretKey, hashData.toString());
        query.append("&vnp_SecureHash=").append(vnp_SecureHash);

        String paymentUrl = VNPayConfig.vnp_PayUrl + "?" + query.toString();

        Payment payment = Payment.builder()
                .order(order)
                .paymentMethod("VNPAY")
                .amount(amount)
                .vnpayTransactionRef(vnp_TxnRef)
                .status(PaymentStatus.PENDING)
                .createdDate(LocalDateTime.now())
                .build();
        paymentRepository.save(payment);

        VNPayPaymentResponse paymentResponse = new VNPayPaymentResponse();
        paymentResponse.setCode("00");
        paymentResponse.setMessage("Success");
        paymentResponse.setPaymentUrl(paymentUrl);
        paymentResponse.setTransactionRef(vnp_TxnRef);

        return ResponseEntity.ok().body(paymentResponse);
    }

    private ResponseEntity<?> handleCashPayment(Order order) {
        Payment payment = Payment.builder()
                .order(order)
                .paymentMethod("CASH")
                .amount(order.getTotalAmount())
                .status(PaymentStatus.PENDING)
                .createdDate(LocalDateTime.now())
                .build();
        paymentRepository.save(payment);

        PaymentResponse response = PaymentResponse.builder()
                .id(payment.getId())
                .orderId(order.getId())
                .paymentMethod("CASH")
                .amount(payment.getAmount())
                .status("PENDING")
                .build();

        return ResponseEntity.ok().body(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @PostMapping("/cash/confirm")
    public ResponseEntity<?> confirmCashPayment(@RequestParam("orderId") Integer orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        Payment payment = paymentRepository.findByOrder(order)
                .orElseThrow(() -> new RuntimeException("Payment not found for order"));

        if (!"CASH".equals(payment.getPaymentMethod())) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Payment method is not CASH")
            );
        }

        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setPaymentDate(LocalDateTime.now());
        paymentRepository.save(payment);

        order.setStatus(OrderStatus.APPROVED);
        orderRepository.save(order);

        PaymentResponse response = PaymentResponse.builder()
                .id(payment.getId())
                .orderId(order.getId())
                .paymentMethod("CASH")
                .amount(payment.getAmount())
                .status("COMPLETED")
                .build();

        return ResponseEntity.ok().body(response);
    }

    @Transactional
    @GetMapping("/payment_infor")
    public ResponseEntity<?> paymentInfor(
            @RequestParam Map<String, String> allParams) {

        String responseCode = allParams.get("vnp_ResponseCode");
        String vnpayTransactionRef = allParams.get("vnp_TxnRef");
        String transactionNo = allParams.get("vnp_TransactionNo");

        VNPayApiResponse vnpayApiResponse = new VNPayApiResponse();

        try {
            Payment payment = paymentRepository.findByVnpayTransactionRef(vnpayTransactionRef)
                    .orElseThrow(() -> new RuntimeException("Payment not found"));

            Order order = payment.getOrder();

            if ("00".equals(responseCode)) {
                // THANH TOÁN THÀNH CÔNG
                payment.setStatus(PaymentStatus.COMPLETED);
                payment.setTransactionId(transactionNo);
                payment.setPaymentDate(LocalDateTime.now());
                payment.setResponseData(allParams.toString());
                paymentRepository.save(payment);

                order.setStatus(OrderStatus.APPROVED);
                orderRepository.save(order);

                vnpayApiResponse.setCode("OK");
                vnpayApiResponse.setMessage("Thanh toán thành công! Đơn hàng #" + order.getId() + " đã được xác nhận.");

            } else {
                // THANH TOÁN THẤT BẠI - HUỶ ORDER
                payment.setStatus(PaymentStatus.FAILED);
                payment.setResponseData(allParams.toString());
                paymentRepository.save(payment);
                cancelOrderAndRestoreStock(order);

                vnpayApiResponse.setCode("NO");
                vnpayApiResponse.setMessage("Thanh toán thất bại! Đơn hàng #" + order.getId() + " đã bị huỷ. Mã lỗi: " + responseCode);
            }

        } catch (Exception e) {
            vnpayApiResponse.setCode("ERROR");
            vnpayApiResponse.setMessage("Lỗi xử lý: " + e.getMessage());
        }

        return ResponseEntity.ok().body(vnpayApiResponse);
    }

    @PutMapping("/{paymentId}/status")
    public ResponseEntity<?> updatePaymentStatus(
            @PathVariable Integer paymentId,
            @RequestParam PaymentStatus status) {  // URL: /api/payment/10/status?status=COMPLETED

        PaymentResponse paymentResponse = paymentService.updatePaymentStatus(paymentId, status);
        return ResponseEntity.ok(paymentResponse);
    }

    // HUỶ ORDER, TRẢ LẠI STOCK
    private void cancelOrderAndRestoreStock(Order order) {
        try {
            // Trả lại stock cho từng sản phẩm
            for (OrderDetail detail : order.getOrderDetails()) {
                ProductVariation variation = detail.getProductVariation();
                variation.setStockQuantity(variation.getStockQuantity() + detail.getQuantity());
            }

            // Cập nhật order status thành CANCELED
            order.setStatus(OrderStatus.CANCELED);
            orderRepository.save(order);

        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi huỷ order #" + order.getId(), e);
        }
    }
}