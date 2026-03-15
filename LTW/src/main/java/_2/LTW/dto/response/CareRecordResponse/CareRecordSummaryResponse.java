package _2.LTW.dto.response.CareRecordResponse;

import _2.LTW.entity.CareBooking.CareBookingStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CareRecordSummaryResponse {

    Long id;
    Long careBookingId;
    Integer petId;
    String petName;
    Long doctorId;
    String doctorName;
    LocalDate bookingDate;
    LocalTime startTime;
    CareBookingStatus bookingStatus;
    BigDecimal totalCost;
    LocalDateTime createdAt;
}

