package _2.LTW.validate;

import _2.LTW.dto.request.doctor_work.SlotRequest;
import _2.LTW.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class WeeklyScheduleValidator {

    Clock clock;

    public void validateSlots(List<SlotRequest> slots){

//        if(slots == null || slots.isEmpty()){
//            throw ErrorCode.BAD_REQUEST.toException("Lịch không được để trống");
//        }

        if(slots.size() > 14){
            throw ErrorCode.BAD_REQUEST.toException("Đăng kí tối đa 14 buổi một tuần");
        }

        Set<String> unique = new HashSet<>();

        for(SlotRequest slot : slots){
            String key = slot.getDayOfWeek() + "_" + slot.getShiftType();

            if(!unique.add(key)){
                throw ErrorCode.BAD_REQUEST.toException("Không được đăng kí 2 ca trùng nhau");
            }
        }

    }

    public void validateViewWeekStart(LocalDate weekStart){

//        LocalDate today = LocalDate.now();

        LocalDate today = LocalDate.now(clock);

        LocalDate currentMonday = today.with(DayOfWeek.MONDAY);
        LocalDate nextWeekMonday = currentMonday.plusWeeks(1);

        boolean isBeforeOrEqualWednesday = today.getDayOfWeek().getValue() <= DayOfWeek.WEDNESDAY.getValue();

        if(isBeforeOrEqualWednesday && weekStart.isAfter(currentMonday)){
            throw ErrorCode.BAD_REQUEST.toException("Chỉ đặt lịch trong tuần này");
        }

        if(!isBeforeOrEqualWednesday && weekStart.isAfter(nextWeekMonday)){
            throw ErrorCode.BAD_REQUEST.toException("Chỉ được đặt lịch trong tuần này hoặc tuần sau");
        }

    }

}
