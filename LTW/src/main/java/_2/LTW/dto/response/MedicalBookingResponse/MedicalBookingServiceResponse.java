package _2.LTW.dto.response.MedicalBookingResponse;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MedicalBookingServiceResponse {
    Integer id;
    Long medicalServiceId;
    String medicalServiceName;
    Integer timeDuration;
    String notes;
}
