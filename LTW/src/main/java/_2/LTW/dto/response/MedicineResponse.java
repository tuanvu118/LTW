package _2.LTW.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)

public class MedicineResponse {
    Integer id;
    String name;
    Double price;
    String usageInstruction;
}
