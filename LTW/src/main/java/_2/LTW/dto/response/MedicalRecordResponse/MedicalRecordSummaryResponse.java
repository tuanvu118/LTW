package _2.LTW.dto.response.MedicalRecordResponse;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MedicalRecordSummaryResponse {
    Long id;
    Integer medicalBookingId;
    Integer petId;
    String petName;
    Long doctorId;
    String doctorName;
    LocalDate bookingDate;
    String diagnosis;
    String conclusion;
    BigDecimal totalCost;
    LocalDateTime createdAt;
}
