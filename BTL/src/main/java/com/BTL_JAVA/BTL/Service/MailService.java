package com.BTL_JAVA.BTL.Service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MailService implements EmailService {

    final JavaMailSender mailSender;

    @Value("${app.mail.from:no-reply@example.com}")
    String fromEmail;

    @Override
    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Password reset request");
        message.setText(buildBody(resetLink));

        try {
            mailSender.send(message);
        } catch (MailException ex) {
            log.error("Failed to send reset password email to {}", toEmail);
            log.debug("Mail send exception", ex);
        }
    }

    private String buildBody(String resetLink) {
        return "Chung toi da nhan duoc yeu cau dat lai mat khau.\n\n"
                + "Nhan vao link ben duoi de dat lai mat khau:\n"
                + resetLink
                + "\n\nLink se het han sau mot thoi gian ngan.";
    }
}


