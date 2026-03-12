package _2.LTW.dto.response.MedicalRecordResponse;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PrescriptionResponse {
    Long id;
    Integer medicineId;
    String medicineName;
    Integer quantity;
    BigDecimal price;
    String dosage;
    String note;
}
