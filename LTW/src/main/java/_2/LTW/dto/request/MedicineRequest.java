package _2.LTW.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MedicineRequest {
    @NotBlank(message = "Không được để trống tên thuốc")
    String name;

    @NotNull(message = "Không được để trống giá thuốc")
    @Positive(message = "Giá thuốc phải lớn hơn 0")
    BigDecimal price;

    String usageInstruction;
}
