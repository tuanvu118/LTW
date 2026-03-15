package _2.LTW.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class LoginResponse {
    private String access_token;
    private int expires_in;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String refreshTokenRaw;
    private UserResponse userResponse;
}
