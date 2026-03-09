package _2.LTW.dto.response.MedicalBookingResponse;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DoctorAvailabilityResponse {
    Long doctorId;
    String doctorName;
    LocalDate bookingDate;
    LocalTime startTime;
    LocalTime estimatedEndTime;
    Integer totalDuration;
}
