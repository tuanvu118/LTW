package com.BTL_JAVA.BTL.Service;

public interface EmailService {
    void sendPasswordResetEmail(String toEmail, String resetLink);
}

