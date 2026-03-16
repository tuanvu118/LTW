package _2.LTW.dto.response.DoctorDailySlotResponse;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DoctorSlotResponse {

    Long doctorId;

    String doctorName;

    String imageUrl;

}
