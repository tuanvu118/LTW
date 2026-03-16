package _2.LTW.entity.DoctorDailySlot;

import _2.LTW.entity.User;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class DoctorDailySlotId implements Serializable {

    User doctor;

    LocalDate slotDate;

    LocalTime slotTime;

}
