package _2.LTW.dto.request.CareServiceRequest;

import jakarta.validation.constraints.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CareServiceUpdateRequest {

    @Size(max = 100, message = "Tên dịch vụ không quá 100 ký tự")
    String name;

    String description;

    @DecimalMin(value = "0.0", inclusive = false, message = "Giá dịch vụ phải lớn hơn 0")
    BigDecimal price;

    @Min(value = 1, message = "Thời gian dịch vụ phải lớn hơn 0")
    Integer durationMinutes;

    Set<String> species;
}

