package _2.LTW.service;

import _2.LTW.dto.request.RegisterRequest;
import _2.LTW.dto.request.LoginRequest;
import _2.LTW.dto.response.LoginResponse;
import _2.LTW.dto.response.UserResponse;
import _2.LTW.entity.User;
import _2.LTW.repository.UserRepository;
import _2.LTW.repository.RoleRepository;
import _2.LTW.util.JwtUtil;
import _2.LTW.exception.ErrorCode;
import _2.LTW.validate.EmailValidate;
import _2.LTW.exception.AppException;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;



@Service
@Getter
@Setter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

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
        user.setRole(roleRepository.findByName("user").orElseThrow(() -> ErrorCode.ROLE_NOT_FOUND.toException()));
        user.setCreatedAt(LocalDateTime.now());
        userRepository.save(user);
        return user;
    }


    private final JwtUtil jwtUtil;

    public LoginResponse login(LoginRequest loginRequest) {
        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> ErrorCode.INVALID_CREDENTIALS.toException());
        
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw ErrorCode.INVALID_CREDENTIALS.toException();
        }
        
        String roleName = user.getRole() != null ? user.getRole().getName() : "user";
        
        String token = jwtUtil.generateToken(user.getUsername(), user.getId(), roleName);
        
        return LoginResponse.builder()
            .token(token)
            .userResponse(UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .isDeleted(user.getIsDeleted())
                .build())
            .build();
    }
}
