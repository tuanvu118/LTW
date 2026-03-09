package _2.LTW.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;
import jakarta.validation.constraints.*;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TreatmentMethodsRequest {
    @NotBlank(message = "Tên phương pháp điều trị không được để trống")
    @Size(max = 255, message = "Tên phương pháp điều trị không được vượt quá 255 ký tự")
    String name;

    @NotNull(message = "Giá phương pháp điều trị không được để trống")
    @Positive(message = "Giá phương pháp điều trị phải lớn hơn 0")
    Integer price;

}
