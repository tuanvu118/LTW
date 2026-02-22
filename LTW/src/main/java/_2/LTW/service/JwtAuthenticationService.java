package _2.LTW.service;

import _2.LTW.entity.User;
import _2.LTW.entity.UserRole;
import _2.LTW.entity.Role;
import _2.LTW.enums.RoleEnum;
import _2.LTW.repository.UserRoleRepository;
import _2.LTW.repository.UserRepository;
import _2.LTW.util.CustomPrincipal;
import _2.LTW.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;
/**
 * Service chứa tất cả logic xử lý JWT Authentication
 * Được sử dụng bởi cả Filter và Interceptor để tránh code trùng lặp
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JwtAuthenticationService {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    // Tên header chứa JWT token
    private static final String AUTH_HEADER = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";

    // Tên cookie chứa JWT token (đọc từ application.properties)
    @Value("${jwt.cookie.name:jwtToken}")
    private String jwtCookieName;

    /**
     * Hàm 1: Lấy JWT token từ Authorization header
     * Nhiệm vụ: Đọc header "Authorization" và extract token sau prefix "Bearer "
     * 
     * @param request HttpServletRequest chứa header
     * @return Token string nếu tìm thấy, null nếu không có
     */
    public String extractTokenFromHeader(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTH_HEADER);
        if (authHeader != null && authHeader.startsWith(TOKEN_PREFIX)) {
            return authHeader.substring(TOKEN_PREFIX.length());
        }
        return null;
    }

    /**
     * Hàm 2: Validate JWT token
     * Nhiệm vụ: Kiểm tra token có hợp lệ không (chữ ký, thời gian hết hạn)
     * 
     * @param token JWT token string
     * @return true nếu token hợp lệ, false nếu không
     */
    public boolean validateToken(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        return jwtUtil.validateToken(token);
    }

    /**
     * Hàm 3: Lấy username từ JWT token
     * Nhiệm vụ: Extract claim "sub" (subject) từ token payload
     * 
     * @param token JWT token string
     * @return Username string
     */
    public String getUsernameFromToken(String token) {
        return jwtUtil.getUsernameFromToken(token);
    }

    /**
     * Hàm 4: Lấy user ID từ JWT token
     * Nhiệm vụ: Extract claim "userId" từ token payload
     * 
     * @param token JWT token string
     * @return User ID (Long)
     */
    public Long getUserIdFromToken(String token) {
        return jwtUtil.getUserIdFromToken(token);
    }

    /**
     * Hàm 5: Lấy role name từ JWT token
     * Nhiệm vụ: Extract claim "role" từ token payload, nếu không có thì query database
     * 
     * @param token JWT token string
     * @param username Username để query database nếu cần
     * @return Role name string (ví dụ: "admin", "doctor", "user")
     */
    public String getRoleFromToken(String token, String username) {
        String roleName = jwtUtil.getRoleFromToken(token);
        
        if (roleName == null || roleName.isEmpty()) {
            log.debug("Role không có trong token, đang query từ database cho user: {}", username);
            Optional<User> userOptional = userRepository.findByUsername(username);
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                var roles = userRoleRepository.findByUser_Id(user.getId()).stream()
                        .map(UserRole::getRole)
                        .collect(Collectors.toList());
                roleName = roles.stream()
                        .map(Role::getRoleEnum)
                        .map(RoleEnum::name)
                        .collect(Collectors.joining(", "));
                log.debug("Role từ database: {}", roleName);
            } else {
                roleName = "USER"; // Default role
                log.warn("Không tìm thấy user trong database, dùng role mặc định: user");
            }
        }
        
        return roleName;
    }

    /**
     * Hàm 6: Chuyển đổi role name thành Spring Security authority
     * Nhiệm vụ: Thêm prefix "ROLE_" và chuyển sang UPPERCASE để Spring Security nhận diện
     * Ví dụ: "admin" -> "ROLE_ADMIN"
     * 
     * @param roleName Role name từ token hoặc database
     * @return Authority string với format "ROLE_XXX"
     */
    public String convertRoleToAuthority(String roleName) {
        if (roleName == null || roleName.isEmpty()) {
            roleName = "user"; // Default role
        }
        return "ROLE_" + roleName.toUpperCase();
    }

    /**
     * Hàm 7: Tạo Spring Security Authentication object
     * Nhiệm vụ: Tạo UsernamePasswordAuthenticationToken với username và authorities để Spring Security sử dụng
     * 
     * @param username Username từ token
     * @param roleName Role name từ token hoặc database
     * @param request HttpServletRequest để set details
     * @return UsernamePasswordAuthenticationToken object
     */
    public UsernamePasswordAuthenticationToken createAuthentication(
            Long userId,
            String username, 
            String roleName, 
            HttpServletRequest request) {

        CustomPrincipal principal = new CustomPrincipal(userId, username, roleName);
        String authorityString = convertRoleToAuthority(roleName);
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority(authorityString);
        
        UsernamePasswordAuthenticationToken authentication = 
            new UsernamePasswordAuthenticationToken(
                principal,
                null,
                Collections.singletonList(authority)
            );
        
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        
        return authentication;
    }

    /**
     * Hàm 8: Set authentication vào Spring Security Context
     * Nhiệm vụ: Lưu authentication object vào SecurityContextHolder để Spring Security sử dụng
     * 
     * @param authentication UsernamePasswordAuthenticationToken object
     */
    public void setSecurityContext(UsernamePasswordAuthenticationToken authentication) {
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /**
     * Hàm 9: Set username và userId vào request attributes
     * Nhiệm vụ: Lưu thông tin user vào request để controller có thể truy cập qua request.getAttribute()
     * 
     * @param request HttpServletRequest
     * @param username Username từ token
     * @param userId User ID từ token
     */
    public void setRequestAttributes(HttpServletRequest request, String username, Long userId) {
        request.setAttribute("username", username);
        request.setAttribute("userId", userId);
    }

    /**
     * Hàm 10: Gửi response 401 Unauthorized
     * Nhiệm vụ: Trả về HTTP 401 với message lỗi dạng JSON
     * 
     * @param response HttpServletResponse
     * @param message Error message
     */
    public void sendUnauthorizedResponse(HttpServletResponse response, String message) {
        try {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(
                String.format("{\"error\": \"Không được phép\", \"message\": \"%s\"}", message)
            );
            response.getWriter().flush();
        } catch (IOException e) {
            log.error("Lỗi khi gửi response không được phép: {}", e.getMessage());
        }
    }

    /**
     * Hàm 11: Xử lý JWT authentication cho Spring Security Filter
     * Nhiệm vụ: Tổng hợp các bước: extract token -> validate -> create authentication -> set vào SecurityContext
     * 
     * @param request HttpServletRequest
     * @return true nếu authentication thành công, false nếu không
     */
    public boolean processJwtAuthenticationForFilter(HttpServletRequest request) {
        try {
            String token = extractTokenFromHeader(request);
            if (token == null) {
                log.debug("Không có token trong request: {}", request.getRequestURI());
                return false;
            }

            if (!validateToken(token)) {
                log.warn("Token không hợp lệ cho request: {}", request.getRequestURI());
                return false;
            }

            Long userId = getUserIdFromToken(token);

            String username = getUsernameFromToken(token);
            log.debug("Username từ token: {}", username);

            String roleName = getRoleFromToken(token, username);
            log.debug("Role từ token/database: {}", roleName);

            UsernamePasswordAuthenticationToken authentication = 
                createAuthentication(userId, username, roleName, request);
            setSecurityContext(authentication);

            log.info("✅ Đã xác thực user: {} với role: {}, authority: {} cho request: {}", 
                    username, roleName, convertRoleToAuthority(roleName), request.getRequestURI());
            
            return true;

        } catch (Exception e) {
            log.error("Lỗi khi xác thực token cho request {}: {}", request.getRequestURI(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * Hàm 12: Xử lý JWT authentication cho Spring MVC Interceptor
     * Nhiệm vụ: Tổng hợp các bước: extract token -> validate
     * 
     * @param request HttpServletRequest
     * @param response HttpServletResponse (để gửi error response nếu cần)
     * @return true nếu authentication thành công, false nếu không
     */
    public boolean processJwtAuthenticationForInterceptor(
            HttpServletRequest request, 
            HttpServletResponse response) {
        
        try {
            // Bước 1: Lấy token từ header
            String token = extractTokenFromHeader(request);
            if (token == null) {
                log.warn("Thiếu hoặc không hợp lệ header Authorization cho request: {}", request.getRequestURI());
                sendUnauthorizedResponse(response, "Thiếu hoặc không hợp lệ header Authorization");
                return false;
            }

            // Bước 2: Validate token
            if (!validateToken(token)) {
                log.warn("Token không hợp lệ hoặc đã hết hạn cho request: {}", request.getRequestURI());
                sendUnauthorizedResponse(response, "Token không hợp lệ hoặc đã hết hạn");
                return false;
            }

            // Bước 3: Lấy thông tin user từ token để log (không set vào request attributes)
            String username = getUsernameFromToken(token);
            Long userId = getUserIdFromToken(token);

            log.info("Người dùng đã xác thực - Username: {}, User ID: {} cho request: {}", 
                    username, userId, request.getRequestURI());
            
            return true;

        } catch (Exception e) {
            log.error("Lỗi khi xác thực token: {}", e.getMessage());
            sendUnauthorizedResponse(response, "Lỗi khi xác thực token: " + e.getMessage());
            return false;
        }
    }
}
