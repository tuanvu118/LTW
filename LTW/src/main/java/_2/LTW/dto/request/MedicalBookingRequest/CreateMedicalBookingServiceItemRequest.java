package _2.LTW.dto.request.MedicalBookingRequest;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateMedicalBookingServiceItemRequest {
    @NotNull
    Long medicalServiceId;

    String notes;
}
