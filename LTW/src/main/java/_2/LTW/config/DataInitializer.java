package _2.LTW.config;

import _2.LTW.entity.Role;
import _2.LTW.entity.User;
import _2.LTW.repository.RoleRepository;
import _2.LTW.repository.UserRepository;
import _2.LTW.enums.RoleEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Class tự động chạy khi ứng dụng khởi động
 * Tạo tài khoản admin nếu chưa có
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Override
    public void run(String... args) throws Exception {
        // Khởi tạo 3 role trước: admin, doctor, user
        initializeRoles();
        // Sau đó mới tạo admin user
        initializeAdminUser();
    }

    /**
     * Khởi tạo 3 role cơ bản: admin, doctor, user
     * Kiểm tra từng role, nếu chưa có thì tạo mới
     */
    private void initializeRoles() {
        try {
            log.info("Đang kiểm tra và khởi tạo các role cơ bản...");
            
            // Danh sách các role cần tạo
            RoleEnum[] requiredRoles = {RoleEnum.ADMIN, RoleEnum.DOCTOR, RoleEnum.USER};
            
            for (RoleEnum role : requiredRoles) {
                // Kiểm tra role đã tồn tại chưa
                if (roleRepository.findByRoleEnum(role).isEmpty()) {
                    // Nếu chưa có, tạo mới
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

    /**
     * Khởi tạo tài khoản admin nếu chưa có
     * Lưu ý: Phải gọi initializeRoles() trước để đảm bảo role admin đã tồn tại
     */
    private void initializeAdminUser() {
        try {
            // Tìm role admin (đã được khởi tạo ở initializeRoles())
            Role adminRole = roleRepository.findByRoleEnum(RoleEnum.ADMIN)
                    .orElseThrow(() -> new RuntimeException(
                            "Role 'admin' chưa được khởi tạo. Vui lòng kiểm tra lại initializeRoles()"));

            // Kiểm tra xem đã có user nào có role admin chưa
            List<User> allUsers = userRepository.findAll();
            boolean hasAdminUser = allUsers.stream()
                    .anyMatch(user -> user.getRole() != null 
                            && RoleEnum.ADMIN.name().equalsIgnoreCase(user.getRole().getRoleEnum().name()));

            if (!hasAdminUser) {
                // Tạo tài khoản admin
                log.info("Không tìm thấy user admin, đang tạo tài khoản admin mặc định...");
                
                User adminUser = new User();
                adminUser.setUsername(RoleEnum.ADMIN.name());
                adminUser.setPassword(passwordEncoder.encode("admin")); // Hash password
                adminUser.setEmail("admin@example.com");
                adminUser.setRole(adminRole);
                adminUser.setCreatedAt(LocalDateTime.now());
                adminUser.setIsDeleted(null);
                
                userRepository.save(adminUser);
                
                log.info("✅ Đã tạo tài khoản admin thành công!");
                log.info("Username: admin");
                log.info("Password: admin");
                log.warn("⚠️ VUI LÒNG ĐỔI MẬT KHẨU ADMIN SAU KHI ĐĂNG NHẬP!");
            } else {
                log.info("Đã tồn tại tài khoản admin trong hệ thống.");
            }
        } catch (Exception e) {
            log.error("Lỗi khi khởi tạo tài khoản admin: {}", e.getMessage(), e);
        }
    }
}
