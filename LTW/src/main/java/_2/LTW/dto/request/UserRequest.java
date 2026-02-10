package _2.LTW.dto.request;

import lombok.*;
import org.springframework.web.multipart.MultipartFile;
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class UserRequest {
    private String email;
    private MultipartFile imageUrl;
}
