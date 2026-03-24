package _2.LTW.dto.request;

import lombok.*;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class LoginRequest {
    private String email;
    private String password;
}
