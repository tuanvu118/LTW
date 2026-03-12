package _2.LTW.dto.response.MedicalRecordResponse;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TreatmentRecordResponse {
    Long id;
    Long treatmentMethodId;
    String treatmentMethodName;
    BigDecimal price;
}
