package _2.LTW.dto.response.DoctorWork;

import lombok.*;
import lombok.experimental.FieldDefaults;

import _2.LTW.entity.DoctorWork.ShiftType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SlotResponse {

    Integer dayOfWeek;

    ShiftType shiftType;

}
