package _2.LTW.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)

public class MedicineResponse {
    Integer id;
    String name;
    BigDecimal price;
    String usageInstruction;
}
