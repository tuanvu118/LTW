package _2.LTW.dto.response.MedicalRecordResponse;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MedicalRecordResponse {
    Long id;
    Integer medicalBookingId;

    Integer petId;
    String petName;

    Long ownerId;
    String ownerName;

    Long doctorId;
    String doctorName;

    LocalDate bookingDate;

    String symptoms;
    String diagnosis;
    String conclusion;
    String diseaseCause;
    String diseaseLevel;
    String treatmentNotes;

    BigDecimal totalCost;
    LocalDateTime createdAt;

    List<PrescriptionResponse> prescriptions;
    List<TreatmentRecordResponse> treatments;
}
