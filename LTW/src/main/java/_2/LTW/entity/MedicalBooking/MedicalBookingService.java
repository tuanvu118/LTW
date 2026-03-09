package _2.LTW.entity.MedicalBooking;

import _2.LTW.entity.MedicalService;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Getter
@Setter
@Table(name = "medical_booking_service")
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MedicalBookingService {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    @Column(name = "time_duration")
    Integer timeDuration;

    @Column(name = "notes")
    String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medical_booking_id", nullable = false)
    MedicalBooking medicalBooking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medical_service_id", nullable = false)
    MedicalService medicalService;
}
