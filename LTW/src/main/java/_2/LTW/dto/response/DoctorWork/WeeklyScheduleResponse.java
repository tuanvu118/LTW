package _2.LTW.dto.response.DoctorWork;

import _2.LTW.entity.DoctorWork.SlotStatus;
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
