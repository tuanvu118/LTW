package _2.LTW.dto.response.doctor_work;

import lombok.*;
import lombok.experimental.FieldDefaults;

import _2.LTW.enums.ShiftType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SlotResponse {

    Integer dayOfWeek;

    ShiftType shiftType;

}
