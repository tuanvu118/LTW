package _2.LTW.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CareBookingResponse {

    Long id;

    PetResponse pet;

    UserResponse doctor;

    LocalDate bookingDate;

    LocalTime startTime;

    String notes;

    String status;

    LocalDateTime createdAt;
}
