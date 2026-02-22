package _2.LTW.validate;

import _2.LTW.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BookingDateTimeValidator {

    Clock clock;

    public void validate(LocalDate bookingDate, LocalTime startTime){

//        LocalDate today = LocalDate.now();
//        LocalTime timeNow = LocalTime.now();

        LocalDate today = LocalDate.now(clock);
        LocalTime timeNow = LocalTime.now(clock);

        if(bookingDate.isBefore(today) || (bookingDate.isEqual(today) && startTime.isBefore(timeNow))){
            throw ErrorCode.BAD_REQUEST.toException("Không được đặt lịch trong quá khứ");
        }

        LocalDate currentMonday = today.with(DayOfWeek.MONDAY);
        LocalDate nextWeekMonday = currentMonday.plusWeeks(1);

        boolean isBeforeOrEqualWednesday = today.getDayOfWeek().getValue() <= DayOfWeek.WEDNESDAY.getValue();

        if(isBeforeOrEqualWednesday){
            if(!isDateInWeek(bookingDate, currentMonday)){
                throw ErrorCode.BAD_REQUEST.toException("Chỉ được đặt lịch trong tuần này");
            }
        }
        else{
            boolean isInCurrentWeek = isDateInWeek(bookingDate, currentMonday);
            boolean isInNextWeek = isDateInWeek(bookingDate, nextWeekMonday);

            if(!isInCurrentWeek && !isInNextWeek){
                throw ErrorCode.BAD_REQUEST.toException("Chỉ được đặt lịch trong tuần này hoặc tuần sau");
            }
        }

    }

    public boolean isDateInWeek(LocalDate date, LocalDate weekStart){
        LocalDate weekEnd = weekStart.plusDays(6);
        return !date.isBefore(weekStart)
                && !date.isAfter(weekEnd);
    }

}
