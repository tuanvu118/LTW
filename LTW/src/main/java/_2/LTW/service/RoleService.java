package _2.LTW.service;

import _2.LTW.dto.response.RoleResponse;
import _2.LTW.dto.request.RoleRequest;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import _2.LTW.repository.RoleRepository;
import _2.LTW.repository.UserRepository;
import _2.LTW.dto.response.MessageResponse;
import _2.LTW.entity.User;
import _2.LTW.entity.Role;
import _2.LTW.enums.RoleEnum;
import _2.LTW.repository.UserRoleRepository;
import _2.LTW.entity.UserRole;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Getter
@Setter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)

public class RoleService {
    
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    public List<RoleResponse> getAllRole() {
        
        return roleRepository.findAll().stream()
                .map(role -> new RoleResponse(role.getId(), role.getRoleEnum().name(), role.getDescription()))
                .collect(Collectors.toList());
    }

    public MessageResponse addRoleToUser(RoleRequest roleRequest) {
        User user = userRepository.findById(roleRequest.getUserId()).orElseThrow(() -> new RuntimeException("User not found"));
        Role role = roleRepository.findByRoleEnum(RoleEnum.valueOf(roleRequest.getRoleName().toUpperCase())).orElseThrow(() -> new RuntimeException("Role not found"));
        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(role);
        userRoleRepository.save(userRole);
        userRepository.save(user);
        return new MessageResponse("Vai trò đã được thêm vào người dùng thành công");
    }
}
