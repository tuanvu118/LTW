package com.BTL_JAVA.BTL.Service;

import com.BTL_JAVA.BTL.DTO.Request.Auth.ForgotPasswordRequest;
import com.BTL_JAVA.BTL.DTO.Request.Auth.ResetPasswordRequest;
import com.BTL_JAVA.BTL.Entity.PasswordResetToken;
import com.BTL_JAVA.BTL.Entity.User;
import com.BTL_JAVA.BTL.Exception.AppException;
import com.BTL_JAVA.BTL.Exception.ErrorCode;
import com.BTL_JAVA.BTL.Repository.InvalidtedTokenRepository;
import com.BTL_JAVA.BTL.Repository.PasswordResetTokenRepository;
import com.BTL_JAVA.BTL.Repository.RoleRepository;
import com.BTL_JAVA.BTL.Repository.UserRepository;
import com.BTL_JAVA.BTL.Repository.httpclient.OutboundIdentityClient;
import com.BTL_JAVA.BTL.Repository.httpclient.OutboundUserClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServicePasswordResetTest {

    @Mock
    UserRepository userRepository;
    @Mock
    OutboundIdentityClient outboundIdentityClient;
    @Mock
    InvalidtedTokenRepository invalidtedTokenRepository;
    @Mock
    OutboundUserClient outboundUserClient;
    @Mock
    RoleRepository roleRepository;
    @Mock
    PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock
    EmailService emailService;
    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks
    AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authenticationService, "RESET_PASSWORD_BASE_URL", "http://localhost:5173/reset-password");
        ReflectionTestUtils.setField(authenticationService, "RESET_PASSWORD_EXPIRY_MINUTES", 15L);
    }

    @Test
    void requestPasswordResetShouldStoreTokenAndSendEmailWhenUserExists() {
        User user = User.builder().id(1).email("test@example.com").build();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        authenticationService.requestPasswordReset(ForgotPasswordRequest.builder().email("test@example.com").build());

        verify(passwordResetTokenRepository).deleteByUser(user);
        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));

        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendPasswordResetEmail(eq("test@example.com"), linkCaptor.capture());

        String link = linkCaptor.getValue();
        assertTrue(link.startsWith("http://localhost:5173/reset-password?token="));
        assertTrue(link.length() > "http://localhost:5173/reset-password?token=".length());
    }

    @Test
    void resetPasswordShouldUpdateUserAndMarkTokenUsed() {
        String rawToken = "sample-reset-token";
        User user = User.builder().id(1).password("old").email("test@example.com").build();
        PasswordResetToken token = PasswordResetToken.builder()
                .tokenHash(sha256(rawToken))
                .user(user)
                .used(false)
                .expiryTime(Instant.now().plus(10, ChronoUnit.MINUTES))
                .build();

        when(passwordResetTokenRepository.findByTokenHash(sha256(rawToken))).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("newPassword123")).thenReturn("encodedPwd");

        authenticationService.resetPassword(ResetPasswordRequest.builder()
                .token(rawToken)
                .newPassword("newPassword123")
                .build());

        assertEquals("encodedPwd", user.getPassword());
        assertTrue(token.isUsed());
        verify(userRepository).save(user);
        verify(passwordResetTokenRepository).save(token);
    }

    @Test
    void resetPasswordShouldThrowWhenTokenExpired() {
        String rawToken = "expired-token";
        PasswordResetToken token = PasswordResetToken.builder()
                .tokenHash(sha256(rawToken))
                .user(User.builder().id(1).build())
                .used(false)
                .expiryTime(Instant.now().minus(1, ChronoUnit.MINUTES))
                .build();

        when(passwordResetTokenRepository.findByTokenHash(sha256(rawToken))).thenReturn(Optional.of(token));

        AppException ex = assertThrows(AppException.class, () -> authenticationService.resetPassword(
                ResetPasswordRequest.builder().token(rawToken).newPassword("newPassword123").build()
        ));

        assertEquals(ErrorCode.EXPIRED_RESET_TOKEN, ex.getErrorCode());
        verify(passwordResetTokenRepository).delete(token);
        verify(userRepository, never()).save(any(User.class));
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte b : hash) {
                result.append(String.format("%02x", b));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}

