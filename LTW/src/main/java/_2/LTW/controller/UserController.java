package _2.LTW.controller;

import _2.LTW.dto.response.UserResponse;

import _2.LTW.repository.UserRepository;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import lombok.*;
import java.util.stream.Collectors;
import java.util.List;


@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)


@RequestMapping("/users")
public class UserController {
    private final UserRepository userRepository;

    @GetMapping("/all")
    public ResponseEntity<List<UserResponse>> getAllUser() {
        return ResponseEntity.ok(userRepository.findAll().stream()
                .map(user -> UserResponse.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .role(user.getRole())
                    .createdAt(user.getCreatedAt())
                    .isDeleted(user.getIsDeleted())
                    .build())
                .collect(Collectors.toList()));
    }
}
