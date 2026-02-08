package _2.LTW.dto.response;

import lombok.*;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class LoginResponse {
    private String token;
    private UserResponse userResponse;
}
