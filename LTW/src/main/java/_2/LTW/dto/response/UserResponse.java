package _2.LTW.dto.response;

import _2.LTW.entity.Role;
import java.time.LocalDateTime;
import lombok.*;
import java.util.List;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private List<Role> roles;
    private String imageUrl;
    private LocalDateTime createdAt;
    private Boolean isDeleted;
}
