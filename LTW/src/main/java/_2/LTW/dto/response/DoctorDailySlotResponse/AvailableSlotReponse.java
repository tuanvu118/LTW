package _2.LTW.dto.response.DoctorDailySlotResponse;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AvailableSlotReponse {

    List<LocalTime> times;

    List<DoctorSlotResponse> doctors;

    List<SlotAvailability> availabilities;

}
