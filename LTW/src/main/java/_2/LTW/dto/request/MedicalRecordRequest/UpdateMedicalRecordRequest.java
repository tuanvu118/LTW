package _2.LTW.dto.request.MedicalRecordRequest;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateMedicalRecordRequest {
    String symptoms;
    String diagnosis;
    String conclusion;
    String diseaseCause;
    String diseaseLevel;
    String treatmentNotes;
}
