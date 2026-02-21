package _2.LTW.dto.request.doctor_work;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WeeklyScheduleRequest {

    @NotEmpty
    @Valid
    List<SlotRequest> slots;

}
