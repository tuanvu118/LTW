package _2.LTW.service;

import _2.LTW.dto.response.UserResponse;
import _2.LTW.entity.User;
import _2.LTW.mapper.UserMapper;
import _2.LTW.repository.UserRepository;
import _2.LTW.dto.request.UserRequest;
import _2.LTW.validate.EmailValidate;
import _2.LTW.util.SecurityUtil;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.util.List;
import org.springframework.stereotype.Service;

@Getter
@Setter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service

public class UserService {

    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;
    private final SecurityUtil securityUtil;
    private final UserMapper userMapper;

    public List<UserResponse> getAllUser() {
        List<User> users = userRepository.findAll();
        return userMapper.toUserResponses(users);
    }

    public UserResponse updateUser(Long id, UserRequest userRequest) {
        if (!securityUtil.isAdmin() && !securityUtil.isOwner(id)) {
            throw new RuntimeException("Bạn không có quyền cập nhật thông tin người dùng này");
        }

        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));

        if (!EmailValidate.isValid(userRequest.getEmail())) {
            throw new RuntimeException("Email không hợp lệ");
        }
        user.setEmail(userRequest.getEmail());

        if (userRequest.getImageUrl() != null) {
            user.setImageUrl(cloudinaryService.upload(userRequest.getImageUrl()).get("url").toString());
        }
        userRepository.save(user);

        return userMapper.toUserResponse(user);
    }
}
