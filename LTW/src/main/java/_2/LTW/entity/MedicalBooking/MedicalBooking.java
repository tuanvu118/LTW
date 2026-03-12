package _2.LTW.entity.MedicalBooking;

import _2.LTW.entity.MedicalRecord.MedicalRecords;
import _2.LTW.entity.Pets.Pets;
import _2.LTW.entity.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import java.time.*;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "medical_booking")
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MedicalBooking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    @Column(name = "booking_date", nullable = false)
    LocalDate bookingDate;

    @Column(name = "start_time", nullable = false)
    LocalTime startTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    Status status;

    @Column(name = "created_at", nullable = false)
    @CreationTimestamp
    LocalDateTime createdAt;

    @Column(name = "delete_at")
    LocalDateTime deleteAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false)
    Pets pets;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "docter_id")
    User doctor;

    @OneToMany(mappedBy = "medicalBooking", fetch = FetchType.LAZY, cascade = { CascadeType.DETACH, CascadeType.MERGE,
            CascadeType.PERSIST, CascadeType.REFRESH })
    List<MedicalBookingService> medicalBookingsService;

    @OneToOne(mappedBy = "medicalBooking", fetch = FetchType.LAZY,
            cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH})
    MedicalRecords medicalRecord;

}
