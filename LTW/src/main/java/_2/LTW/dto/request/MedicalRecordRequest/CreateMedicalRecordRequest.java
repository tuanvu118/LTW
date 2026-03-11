package _2.LTW.dto.request.MedicalRecordRequest;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateMedicalRecordRequest {
    @NotNull
    Integer medicalBookingId;

    String symptoms;
    String diagnosis;
    String conclusion;
    String diseaseCause;
    String diseaseLevel;
    String treatmentNotes;
}
