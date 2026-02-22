package _2.LTW.controller;

import _2.LTW.dto.request.RegisterRequest;
import _2.LTW.dto.response.UserResponse;
import _2.LTW.entity.User;
import _2.LTW.service.AuthService;
import _2.LTW.entity.UserRole;
import _2.LTW.repository.UserRoleRepository;
import _2.LTW.dto.request.LoginRequest;
import _2.LTW.dto.response.LoginResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.PostMapping;
import java.util.stream.Collectors;


@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RestController
public class AuthController {

    private final AuthService authService;
    private final UserRoleRepository userRoleRepository;

    // Đăng kí
    @PostMapping("/register")
    public UserResponse register(@RequestBody RegisterRequest registerRequest) {
        User user = authService.register(registerRequest);
        var roles = userRoleRepository.findByUser_Id(user.getId()).stream()
                .map(UserRole::getRole)
                .collect(Collectors.toList());
        return UserResponse.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .roles(roles)
            .imageUrl(user.getImageUrl())
            .createdAt(user.getCreatedAt())
            .isDeleted(user.getIsDeleted())
            .build();
    }
    
    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest loginRequest) {
        return authService.login(loginRequest);
    }
}
