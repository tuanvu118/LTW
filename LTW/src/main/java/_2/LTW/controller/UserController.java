package _2.LTW.controller;

import _2.LTW.dto.response.UserResponse;

import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import _2.LTW.service.UserService;
import _2.LTW.dto.request.UserRequest;
import org.springframework.web.bind.annotation.*;


import lombok.*;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ModelAttribute;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)


@RequestMapping("/users")
public class UserController {
    private final UserService userService;

    @GetMapping("/all")
    public ResponseEntity<List<UserResponse>> getAllUser() {
        return ResponseEntity.ok(userService.getAllUser());
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserResponse updateUser(@PathVariable Long id, @ModelAttribute UserRequest userRequest) {
        return userService.updateUser(id, userRequest);
    }
}
