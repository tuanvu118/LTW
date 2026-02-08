package _2.LTW.service;

import _2.LTW.dto.response.UserResponse;
import _2.LTW.entity.User;
import _2.LTW.repository.UserRepository;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)

public class UserService {

    private final UserRepository userRepository;

    public List<UserResponse> getAllUser() {
        List<User> users = userRepository.findAll();
        return users.stream()
                .map(user -> UserResponse.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .role(user.getRole())
                    .createdAt(user.getCreatedAt())
                    .isDeleted(user.getIsDeleted())
                    .build())
                .collect(Collectors.toList());
    }
}
