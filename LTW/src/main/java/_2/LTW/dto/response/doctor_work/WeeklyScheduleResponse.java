package _2.LTW.dto.response.doctor_work;

import _2.LTW.enums.SlotStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WeeklyScheduleResponse {

    Long doctorId;

    String doctorName;

    LocalDate applyFromWeek;

    SlotStatus status;

    List<SlotResponse> slots;

}
