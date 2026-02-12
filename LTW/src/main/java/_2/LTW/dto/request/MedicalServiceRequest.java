package _2.LTW.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MedicalServiceRequest {

    @NotBlank(message = "Tên dịch vụ không được để trống")
    String name;

    @NotNull(message = "Thời gian của dịch vụ không được để trống")
    @Positive(message = "Thời gian của dịch vụ phải lớn hơn 0")
    Integer timeDuration;

    String description;

}
