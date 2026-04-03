package com.BTL_JAVA.BTL.configuration;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public class VNPayConfig {

    @Value("${vnpay.pay-url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}")
    private String vnp_PayUrl;

    @Value("${backend.url}")
    private String backendUrl;

    @Value("${vnpay.return-path:api/payment/payment_infor}")
    private String returnPath;

    @Value("${vnpay.tmn-code:MHK69BU3}")
    private String vnp_TmnCode;

    @Value("${vnpay.secret-key:6F6PR8ERL33VSFH7BWYJAHKM2OY2F72Q}")
    private String secretKey;

    @Value("${vnpay.version:2.1.0}")
    private String vnp_Version;

    @Value("${vnpay.command:pay}")
    private String vnp_Command;

    public String getVnp_PayUrl() { return vnp_PayUrl; }
    public String getVnp_ReturnUrl() { return backendUrl + returnPath; }
    public String getVnp_TmnCode() { return vnp_TmnCode; }
    public String getSecretKey() { return secretKey; }
    public String getVnp_Version() { return vnp_Version; }
    public String getVnp_Command() { return vnp_Command; }

    public static String hmacSHA512(final String key, final String data) {

        try {
            if (key == null || data == null) {
                throw new NullPointerException();
            }
            final Mac hmac512 = Mac.getInstance("HmacSHA512");
            byte[] hmacKeyBytes = key.getBytes();
            final SecretKeySpec secretKey = new SecretKeySpec(hmacKeyBytes, "HmacSHA512");
            hmac512.init(secretKey);
            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            byte[] result = hmac512.doFinal(dataBytes);
            StringBuilder hash = new StringBuilder(2 * result.length);
            for (byte b : result) {
                hash.append(String.format("%02x", b & 0xff));
            }
            return hash.toString();

        } catch (Exception ex) {
            return "";
        }

    }

    public static String getIpAddress(HttpServletRequest request) {

        String ipAdress;
        try {
            ipAdress = request.getHeader("X-FORWARDED-FOR");
            if (ipAdress == null) {
                ipAdress = request.getRemoteAddr();
            }

        } catch (Exception e) {
            ipAdress = "Invalid IP:" + e.getMessage();
        }
        return ipAdress;

    }

    public static String getRandomNumber(int len) {
        Random rnd = new Random();
        String chars = "0123456789";
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }

}

