package _2.LTW.dto.request;

import lombok.*;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class RegisterRequest {
    private String fullname;
    private String password;
    private String email;
}
