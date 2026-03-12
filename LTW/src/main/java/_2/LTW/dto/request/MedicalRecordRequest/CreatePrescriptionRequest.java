package _2.LTW.dto.request.MedicalRecordRequest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreatePrescriptionRequest {
    @NotNull
    Integer medicineId;

    @NotNull
    @Positive
    Integer quantity;

    @NotBlank
    String dosage;

    String note;
}
