package _2.LTW.dto.response;

import _2.LTW.entity.Role;
import java.time.LocalDateTime;
import lombok.*;

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
    private Role role;
    private LocalDateTime createdAt;
    private Boolean isDeleted;
}
