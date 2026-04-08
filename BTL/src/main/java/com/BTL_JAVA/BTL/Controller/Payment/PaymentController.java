package com.BTL_JAVA.BTL.Controller.Payment;
import com.BTL_JAVA.BTL.DTO.Request.ApiResponse;
import com.BTL_JAVA.BTL.DTO.Response.Payment.VNPayApiResponse;
import com.BTL_JAVA.BTL.DTO.Response.Payment.VNPayPaymentResponse;
import com.BTL_JAVA.BTL.DTO.Response.Payment.PaymentResponse;
import com.BTL_JAVA.BTL.DTO.Response.Payment.VNPayRedirectInfo;
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
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.servlet.view.RedirectView;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping ("/api/payment")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaymentController {

    @Value("${frontend.url}")
    @NonFinal
    String frontendUrl;

    PaymentService paymentService;

    @PreAuthorize("isAuthenticated()")
    @Transactional
    @PostMapping("/create")
    public ApiResponse<Object> createPayment(
            @RequestParam("orderId") Integer orderId,
            @RequestParam("paymentMethod") String paymentMethod, // "VNPAY" hoặc "CASH"
            @RequestParam(value = "bankCode", defaultValue = "NCB") String bankCode,
            HttpServletRequest request)
    {

        Object data = paymentService.createPayment(orderId, paymentMethod, bankCode, request);

        return ApiResponse.ok(data);

    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/cash/confirm")
    public ApiResponse<PaymentResponse> confirmCashPayment(@RequestParam("orderId") Integer orderId) {

        return ApiResponse.ok(paymentService.confirmCashPayment(orderId));

    }

    @GetMapping("/payment_infor")
    public RedirectView paymentInfor(@RequestParam Map<String, String> params) {
        VNPayRedirectInfo redirectInfo = paymentService.handleVNPayReturn(params);
        String url = frontendUrl + "user?payment=" +
                (redirectInfo.isSuccess() ? "success" : "failed") +
                "&orderCode=" + redirectInfo.getOrderId();
        return new RedirectView(url);
    }

    @PutMapping("/{paymentId}/status")
    public ApiResponse<PaymentResponse> updatePaymentStatus(
            @PathVariable Integer paymentId,
            @RequestParam PaymentStatus status)
    {  // URL: /api/payment/10/status?status=COMPLETED

        return ApiResponse.ok(paymentService.updatePaymentStatus(paymentId, status));

    }

}