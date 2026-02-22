package _2.LTW.service;

import _2.LTW.dto.request.RegisterRequest;
import _2.LTW.dto.request.LoginRequest;
import _2.LTW.dto.response.LoginResponse;
import _2.LTW.dto.response.UserResponse;
import _2.LTW.entity.User;
import _2.LTW.entity.UserRole;
import _2.LTW.entity.Role;
import _2.LTW.mapper.UserMapper;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
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

        UserResponse userResponse = userMapper.toUserResponse(user);
        userResponse.setRoles(roles);

        return LoginResponse.builder()
            .token(token)
            .userResponse(userResponse)
            .build();
    }
}
