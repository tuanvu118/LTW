package _2.LTW.dto.request.CareBookingRequest;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CareBookingUpdateRequest {

    Long doctorId;
    LocalDate bookingDate;
    LocalTime startTime;
    String notes;
    String status;
}

