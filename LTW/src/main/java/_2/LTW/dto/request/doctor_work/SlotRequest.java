package _2.LTW.dto.request.doctor_work;

import _2.LTW.enums.ShiftType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SlotRequest {

    @Min(value = 1, message = "Ngày trong tuần bắt đầu là 1")
    @Max(value = 7, message = "Ngày trong tuần kết thúc là 7")
    Integer dayOfWeek;

    @NotNull
    ShiftType shiftType;

}
