package com.BTL_JAVA.BTL.DTO.Request.Auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ResetPasswordRequest {
    @NotBlank(message = "INVALID_KEY")
    String token;

    @NotBlank(message = "INVALID_KEY")
    @Size(min = 8, message = "INVALID_PASSWORD")
    String newPassword;
}


