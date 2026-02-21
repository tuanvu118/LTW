package _2.LTW.entity;

import _2.LTW.enums.ShiftType;
import _2.LTW.enums.SlotStatus;
import jakarta.persistence.*;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "doctor_working_slots",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_doctor_slot_week",
                    columnNames = {
                            "doctor_id",
                            "day_of_week",
                            "shift_type",
                            "apply_from_week"
                    }
            )
        },
        indexes = {
                @Index(
                        name = "idx_doctor_week",
                        columnList = "doctor_id, apply_from_week"
                ),
                @Index(
                        name = "idx_doctor_slot_status",
                        columnList = "doctor_id, status"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DoctorWork {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    User doctor;

    @Column(name = "day_of_week", nullable = false)
    Integer dayOfWeek;

    @Enumerated(EnumType.STRING)
    @Column(name = "shift_type", nullable = false)
    ShiftType shiftType;

    @Column(name = "apply_from_week", nullable = false)
    LocalDate applyFromWeek;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    SlotStatus slotStatus = SlotStatus.PENDING;

    @CreationTimestamp
    @Column(name = "create_at", updatable = false)
    LocalDateTime createAt;

}
