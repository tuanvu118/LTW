package _2.LTW.dto.response.MedicalRecordResponse;

import _2.LTW.dto.response.MedicalBookingResponse.MedicalBookingResponse;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VisitDetailResponse {
    MedicalBookingResponse booking;
    MedicalRecordResponse medicalRecord;
}
