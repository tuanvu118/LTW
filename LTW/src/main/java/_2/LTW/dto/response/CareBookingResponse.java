package _2.LTW.dto.response;

import _2.LTW.entity.CareBooking.CareBookingStatus;
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
public class CareBookingResponse {

    Long id;
    Integer petId;
    String petName;

    Long doctorId;
    String doctorName;

    LocalDate bookingDate;
    LocalTime startTime;
    LocalTime estimatedEndTime;
    Integer totalDuration;

    String notes;
    CareBookingStatus status;
    LocalDateTime createdAt;

    List<CareBookingServiceItemResponse> services;
}

