package _2.LTW.dto.request.CareBookingRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CareBookingCreateRequest {

    @NotNull
    Integer petId;

    @NotNull
    Long doctorId;

    @NotNull
    LocalDate bookingDate;

    @NotNull
    LocalTime startTime;

    @NotEmpty
    @Valid
    List<CareBookingServiceItemRequest> services;

    String notes;
}

