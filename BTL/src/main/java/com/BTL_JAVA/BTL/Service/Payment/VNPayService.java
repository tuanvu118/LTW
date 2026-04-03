package com.BTL_JAVA.BTL.Service.Payment;

import com.BTL_JAVA.BTL.DTO.Response.Payment.VNPayPaymentResponse;
import com.BTL_JAVA.BTL.Entity.Orders.Order;
import com.BTL_JAVA.BTL.configuration.VNPayConfig;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class VNPayService {

    public VNPayPaymentResponse createPaymentUrl(Order order, String bankCode, HttpServletRequest request){

        double amount = order.getTotalAmount();
        long vnpAmount = (long) (amount * 100);

        String txnRef = VNPayConfig.getRandomNumber(8);
        String ip = VNPayConfig.getIpAddress(request);

        Map<String, String> params = new HashMap<>();
        params.put("vnp_Version", VNPayConfig.vnp_Version);
        params.put("vnp_Command", VNPayConfig.vnp_Command);
        params.put("vnp_TmnCode", VNPayConfig.vnp_TmnCode);
        params.put("vnp_Amount", String.valueOf(vnpAmount));
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_BankCode", bankCode);
        params.put("vnp_TxnRef", txnRef);
        params.put("vnp_OrderInfo", "Thanh toan don hang: " + order.getId() + "Ma: " + txnRef);
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", VNPayConfig.vnp_ReturnUrl);
        params.put("vnp_IpAddr", ip);

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        formatter.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));

        cld.add(Calendar.MINUTE, -2);
        params.put("vnp_CreateDate", formatter.format(cld.getTime()));

        System.out.println("Create: " + cld);

        cld.add(Calendar.MINUTE, 17);
        params.put("vnp_ExpireDate", formatter.format(cld.getTime()));

        System.out.println("Expire: " + cld);

        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();

        for (Iterator<String> itr = fieldNames.iterator(); itr.hasNext();) {
            String field = itr.next();
            String value = params.get(field);

            if (value != null && !value.isEmpty()) {
                hashData.append(field).append('=')
                        .append(URLEncoder.encode(value, StandardCharsets.UTF_8));
                query.append(URLEncoder.encode(field, StandardCharsets.UTF_8))
                        .append('=')
                        .append(URLEncoder.encode(value, StandardCharsets.UTF_8));

                if (itr.hasNext()) {
                    hashData.append('&');
                    query.append('&');
                }
            }
        }

        String securedHash = VNPayConfig.hmacSHA512(VNPayConfig.secretKey, hashData.toString());
        query.append("&vnp_SecureHash=").append(securedHash);

        String paymentUrl = VNPayConfig.vnp_PayUrl + "?" + query;

        return VNPayPaymentResponse.builder()
                .code("00")
                .message("Success")
                .paymentUrl(paymentUrl)
                .transactionRef(txnRef)
                .build();

    }

}
