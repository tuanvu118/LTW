package _2.LTW.dto.request.care_booking;

import jakarta.validation.constraints.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CareBookingCreateRequest {

    @NotNull(message = "Pet ID không được để trống")
    Integer petId;

    @NotNull(message = "Doctor ID không được để trống")
    Long doctorId;

    @NotNull(message = "Ngày đặt lịch không được để trống")
    @FutureOrPresent(message = "Ngày đặt lịch phải từ hôm nay trở đi")
    LocalDate bookingDate;

    @NotNull(message = "Giờ bắt đầu không được để trống")
    LocalTime startTime;

    String notes;
}
