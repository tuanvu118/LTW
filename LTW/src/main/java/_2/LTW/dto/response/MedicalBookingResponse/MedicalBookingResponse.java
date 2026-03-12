package _2.LTW.dto.response.MedicalBookingResponse;

import _2.LTW.entity.MedicalBooking.Status;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MedicalBookingResponse {
    Integer id;
    Integer petId;
    String petName;
    Long doctorId;
    String doctorName;

    LocalDate bookingDate;
    LocalTime startTime;
    LocalTime estimatedEndTime;
    Integer totalDuration;

    Status status;
    LocalDateTime createdAt;

    List<MedicalBookingServiceResponse> services;
}
