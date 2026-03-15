package _2.LTW.service;

import _2.LTW.dto.request.RegisterRequest;
import _2.LTW.dto.request.LoginRequest;
import _2.LTW.dto.response.LoginResponse;
import _2.LTW.dto.response.UserResponse;
import _2.LTW.entity.RefreshTokens;
import _2.LTW.entity.User;
import _2.LTW.entity.UserRole;
import _2.LTW.entity.Role;
import _2.LTW.mapper.UserMapper;
import _2.LTW.repository.RefreshTokenRepository;
import _2.LTW.repository.UserRepository;
import _2.LTW.repository.RoleRepository;
import _2.LTW.repository.UserRoleRepository;

import _2.LTW.util.JwtUtil;
import _2.LTW.exception.ErrorCode;
import _2.LTW.validate.EmailValidate;
import _2.LTW.exception.AppException;
import _2.LTW.enums.RoleEnum;

import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;
import java.util.stream.Collectors;



@Service
@Getter
@Setter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final UserRoleRepository userRoleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    @NonFinal
    @Value("${jwt.expiration:1800000}")
    private Long accessTokenExpirationMs;
    public User register(RegisterRequest userRequest) {
        if (userRepository.findByUsername(userRequest.getUsername()).isPresent()) {
            throw ErrorCode.USERNAME_ALREADY_EXISTS.toException();
        }

        if (!EmailValidate.isValid(userRequest.getEmail())) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Email không hợp lệ");
        }
        
        if (userRepository.findByEmail(userRequest.getEmail()).isPresent()) {
            throw ErrorCode.EMAIL_ALREADY_EXISTS.toException();
        }
        
        User user = new User();
        user.setUsername(userRequest.getUsername());
        String hashedPassword = passwordEncoder.encode(userRequest.getPassword());
        user.setPassword(hashedPassword);
        user.setEmail(userRequest.getEmail());
        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(roleRepository.findByRoleEnum(RoleEnum.USER).orElseThrow(() -> ErrorCode.ROLE_NOT_FOUND.toException()));
        user.setCreatedAt(LocalDateTime.now());
        userRepository.save(user);
        userRoleRepository.save(userRole);
        return user;
    }


    private final JwtUtil jwtUtil;

    public LoginResponse login(LoginRequest loginRequest) {
        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> ErrorCode.INVALID_CREDENTIALS.toException());
        
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw ErrorCode.INVALID_CREDENTIALS.toException();
        }
        
        var roles = userRoleRepository.findByUser_Id(user.getId()).stream()
                .map(UserRole::getRole)
                .collect(Collectors.toList());
        String roleName = roles.stream()
                .map(Role::getRoleEnum)
                .map(RoleEnum::name)
                .collect(Collectors.joining(", "));
        String token = jwtUtil.generateToken(user.getUsername(), user.getId(), roleName);
        String refreshJti = UUID.randomUUID().toString();
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername(), user.getId(), refreshJti);
        saveRefreshToken(user, refreshJti, refreshToken);

        UserResponse userResponse = userMapper.toUserResponse(user);
        userResponse.setRoles(roles);

        return LoginResponse.builder()
            .access_token(token)
            .expires_in_seconds(Math.toIntExact(accessTokenExpirationMs / 1000))
            .refreshTokenRaw(refreshToken)
            .userResponse(userResponse)
            .build();
    }

    @Transactional
    public LoginResponse refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw ErrorCode.INVALID_REFRESH_TOKEN.toException("Thiếu refresh token trong cookie");
        }

        if (!jwtUtil.validateToken(refreshToken)) {
            throw ErrorCode.INVALID_REFRESH_TOKEN.toException("Refresh token không hợp lệ hoặc sai chữ ký");
        }

        String tokenType = jwtUtil.getTypeFromToken(refreshToken);
        if (!"refresh".equals(tokenType)) {
            throw ErrorCode.INVALID_REFRESH_TOKEN.toException("Token không phải refresh token");
        }

        String jti = jwtUtil.getJtiFromToken(refreshToken);
        if (jti == null || jti.isBlank()) {
            throw ErrorCode.INVALID_REFRESH_TOKEN.toException("Refresh token thiếu jti");
        }

        Long userId = jwtUtil.getUserIdFromToken(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ErrorCode.USER_NOT_FOUND.toException());

        RefreshTokens refreshTokenEntity = refreshTokenRepository.findByJti(jti)
                .orElseThrow(() -> ErrorCode.REFRESH_TOKEN_NOT_FOUND.toException("Không tìm thấy refresh token trong DB"));

        if (!refreshTokenEntity.getUser().getId().equals(userId)) {
            throw ErrorCode.INVALID_REFRESH_TOKEN.toException("Refresh token không khớp người dùng");
        }

        if (refreshTokenEntity.getUsedAt() != null) {
            throw ErrorCode.REFRESH_TOKEN_USED.toException("Refresh token đã được dùng trước đó");
        }

        if (refreshTokenEntity.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw ErrorCode.REFRESH_TOKEN_EXPIRED.toException("Refresh token đã hết hạn trong DB");
        }

        String refreshTokenHash = hashToken(refreshToken);
        if (!refreshTokenHash.equals(refreshTokenEntity.getTokenHash())) {
            throw ErrorCode.INVALID_REFRESH_TOKEN.toException("Refresh token không khớp token_hash trong DB");
        }

        // Rotate refresh token: token cũ được đánh dấu đã dùng.
        refreshTokenEntity.setUsedAt(LocalDateTime.now());
        refreshTokenRepository.save(refreshTokenEntity);

        var roles = userRoleRepository.findByUser_Id(user.getId()).stream()
                .map(UserRole::getRole)
                .collect(Collectors.toList());
        String roleName = roles.stream()
                .map(Role::getRoleEnum)
                .map(RoleEnum::name)
                .collect(Collectors.joining(", "));

        String accessToken = jwtUtil.generateToken(user.getUsername(), user.getId(), roleName);
        String newRefreshJti = UUID.randomUUID().toString();
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getUsername(), user.getId(), newRefreshJti);
        saveRefreshToken(user, newRefreshJti, newRefreshToken);

        UserResponse userResponse = userMapper.toUserResponse(user);
        userResponse.setRoles(roles);

        return LoginResponse.builder()
                .access_token(accessToken)
                .expires_in_seconds(Math.toIntExact(accessTokenExpirationMs / 1000))
                .refreshTokenRaw(newRefreshToken)
                .userResponse(userResponse)
                .build();
    }

    private void saveRefreshToken(User user, String jti, String rawRefreshToken) {
        RefreshTokens refreshTokens = new RefreshTokens();
        refreshTokens.setJti(jti);
        refreshTokens.setUser(user);
        refreshTokens.setTokenHash(hashToken(rawRefreshToken));
        refreshTokens.setExpiresAt(toLocalDateTime(jwtUtil.getExpirationDateFromToken(rawRefreshToken)));
        refreshTokens.setUsedAt(null);
        refreshTokens.setCreatedAt(LocalDateTime.now());
        refreshTokenRepository.save(refreshTokens);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte hashedByte : hashedBytes) {
                hex.append(String.format("%02x", hashedByte));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "Không thể hash refresh token");
        }
    }

    private LocalDateTime toLocalDateTime(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
}
