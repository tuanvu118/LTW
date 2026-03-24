package _2.LTW.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Utility class để xử lý JWT token
 */
@Component
@Slf4j
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration:1800000}")
    private Long expiration;

    @Value("${jwt.refresh-expiration:1296000000}")
    private Long refreshExpirationMs;

    /**
     * Tạo secret key từ secret string
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Tạo JWT token từ email và user ID
     */
    public String generateToken(String email, Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        return createToken(claims, email);
    }

    /**
     * Tạo JWT token từ email, user ID, role name và fullname
     */
    public String generateToken(String email, Long userId, String roleName, String fullname) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("role", roleName);
        claims.put("fullname", fullname != null ? fullname : "");
        return createToken(claims, email);
    }

    /**
     * Tạo JWT token
     */
    private String createToken(Map<String, Object> claims, String subject) {
        return createToken(claims, subject, expiration);
    }

    private String createToken(Map<String, Object> claims, String subject, Long expiresInMs) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiresInMs);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }
    
    /**
     * Lấy claim cụ thể từ token
     */
    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Lấy tất cả claims từ token
     */
    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Lấy email (subject) từ token
     */
    public String getEmailFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    /**
     * Lấy fullname từ token
     */
    public String getFullnameFromToken(String token) {
        Object fullname = getClaimFromToken(token, claims -> claims.get("fullname"));
        return fullname != null ? fullname.toString() : null;
    }

    /**
     * Lấy expiration date từ token
     */
    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    /**
     * Lấy role từ token
     */
    public String getRoleFromToken(String token) {
        Object roleObj = getClaimFromToken(token, claims -> claims.get("role"));
        if (roleObj == null) {
            return null;
        }
        return roleObj.toString();
    }

    /**
     * Lấy user ID từ token
     */
    public Long getUserIdFromToken(String token) {
        return getClaimFromToken(token, claims -> claims.get("userId", Long.class));
    }

    /**
     * Kiểm tra token có hết hạn không
     */
    private Boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

    /**
     * Validate token với email
     */
    public Boolean validateToken(String token, String email) {
        final String tokenEmail = getEmailFromToken(token);
        return (tokenEmail != null && tokenEmail.equals(email) && !isTokenExpired(token));
    }

    /**
     * Validate token (chỉ kiểm tra format và expiration)
     */
    public Boolean validateToken(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    public String generateRefreshToken(String email, Long userId) {
        return generateRefreshToken(email, userId, UUID.randomUUID().toString());
    }

    public String generateRefreshToken(String email, Long userId, String jti) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("type", "refresh");
        claims.put("jti", jti);
        return createToken(claims, email, refreshExpirationMs);
    }

    public String getTypeFromToken(String token) {
        return getClaimFromToken(token, claims -> claims.get("type", String.class));
    }

    public String getJtiFromToken(String token) {
        return getClaimFromToken(token, claims -> claims.get("jti", String.class));
    }
}
