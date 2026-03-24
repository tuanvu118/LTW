package _2.LTW.config;

import _2.LTW.entity.Role;
import _2.LTW.entity.User;
import _2.LTW.entity.UserRole;
import _2.LTW.repository.RoleRepository;
import _2.LTW.repository.UserRepository;
import _2.LTW.repository.UserRoleRepository;
import _2.LTW.enums.RoleEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Tự động chạy khi ứng dụng khởi động xong (sau khi DB sẵn sàng).
 * Tạo tài khoản admin nếu chưa có.
 * Dùng ApplicationReadyEvent thay vì CommandLineRunner để đảm bảo DB đã sẵn sàng.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    @EventListener(ApplicationReadyEvent.class)
    @Order(Integer.MIN_VALUE)
    public void onApplicationReady() {
        initializeRoles();
        initializeAdminUser();
    }

    private void initializeRoles() {
        try {
            log.info("Đang kiểm tra và khởi tạo các role cơ bản...");
            RoleEnum[] requiredRoles = {RoleEnum.ADMIN, RoleEnum.DOCTOR, RoleEnum.USER};

            for (RoleEnum role : requiredRoles) {
                if (roleRepository.findByRoleEnum(role).isEmpty()) {
                    log.info("Không tìm thấy role '{}', đang tạo mới...", role.name());
                    Role newRole = new Role();
                    newRole.setRoleEnum(role);
                    newRole.setDescription("Role " + role.name());
                    roleRepository.save(newRole);
                    log.info("✅ Đã tạo role '{}' thành công", role.name());
                } else {
                    log.debug("Role '{}' đã tồn tại trong hệ thống", role.name());
                }
            }
            log.info("Hoàn thành kiểm tra và khởi tạo các role cơ bản.");
        } catch (Exception e) {
            log.error("Lỗi khi khởi tạo các role: {}", e.getMessage(), e);
        }
    }

    private void initializeAdminUser() {
        try {
            Role adminRole = roleRepository.findByRoleEnum(RoleEnum.ADMIN)
                    .orElseThrow(() -> new RuntimeException(
                            "Role 'admin' chưa được khởi tạo. Vui lòng kiểm tra lại initializeRoles()"));

            List<UserRole> userRoles = userRoleRepository.findByRole_Id(adminRole.getId().longValue());

            if (userRoles.isEmpty()) {
                log.info("Không tìm thấy user admin, đang tạo tài khoản admin mặc định...");

                User adminUser = new User();
                adminUser.setFullname("Admin");
                adminUser.setPassword(passwordEncoder.encode("admin"));
                adminUser.setEmail("admin@example.com");
                adminUser.setCreatedAt(LocalDateTime.now());
                adminUser.setIsDeleted(null);
                userRepository.save(adminUser);

                log.info("✅ Đã tạo tài khoản admin thành công!");
                log.info("Email: admin@example.com");
                log.info("Password: admin");
                log.warn("⚠️ VUI LÒNG ĐỔI MẬT KHẨU ADMIN SAU KHI ĐĂNG NHẬP!");
            } else {
                log.info("Đã tồn tại tài khoản admin trong hệ thống.");
            }
        } catch (Exception e) {
            log.error("Lỗi khi khởi tạo tài khoản admin: {}", e.getMessage(), e);
        }

        try {
            Role adminRole = roleRepository.findByRoleEnum(RoleEnum.ADMIN)
                    .orElseThrow(() -> new RuntimeException("Role ADMIN chưa được khởi tạo."));
            User adminUser = userRepository.findByEmail("admin@example.com").orElse(null);
            if (adminUser != null && userRoleRepository.findByUserAndRole(adminUser, adminRole).isEmpty()) {
                UserRole userRole = new UserRole();
                userRole.setUser(adminUser);
                userRole.setRole(adminRole);
                userRoleRepository.save(userRole);
                log.info("✅ Đã liên kết admin với role ADMIN");
            }
        } catch (Exception e) {
            log.error("Lỗi khi tạo bảng user_role: {}", e.getMessage(), e);
        }
    }
}