package _2.LTW.entity.DoctorDailySlot;

import _2.LTW.entity.DoctorWork.ShiftType;
import _2.LTW.entity.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(
        name = "doctor_daily_slots",
        indexes = {
                @Index(
                        name = "idx_slot_lookup",
                        columnList = "slot_date, shift_type, status"
                ),
                @Index(
                        name = "idx_booking",
                        columnList = "booking_id"
                )
        }
)
@IdClass(DoctorDailySlotId.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DoctorDailySlot {

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    User doctor;

    @Id
    @Column(name = "slot_date", nullable = false)
    LocalDate slotDate;

    @Id
    @Column(name = "slot_time", nullable = false)
    LocalTime slotTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "shift_type", nullable = false)
    ShiftType shiftType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    BookingSlotStatus status = BookingSlotStatus.AVAILABLE;

    @Enumerated(EnumType.STRING)
    @Column(name = "booking_type")
    BookingType bookingType;

    @Column(name = "booking_id")
    Long bookingId;

}
