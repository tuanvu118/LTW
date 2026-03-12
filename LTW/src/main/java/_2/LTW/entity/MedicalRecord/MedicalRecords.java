package _2.LTW.entity.MedicalRecord;

import _2.LTW.entity.MedicalBooking.MedicalBooking;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "medical_records")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MedicalRecords {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medical_booking_id", nullable = false, unique = true)
    MedicalBooking medicalBooking;

    @Column(name = "symptoms", columnDefinition = "TEXT")
    String symptoms;

    @Column(name = "diagnosis", columnDefinition = "TEXT")
    String diagnosis;

    @Column(name = "conclusion", columnDefinition = "TEXT")
    String conclusion;

    @Column(name = "disease_cause", columnDefinition = "TEXT")
    String diseaseCause;

    @Column(name = "disease_level", columnDefinition = "TEXT")
    String diseaseLevel;

    @Column(name = "total_cost", precision = 10, scale = 2)
    BigDecimal totalCost;

    @Column(name = "treatment_notes", columnDefinition = "TEXT")
    String treatmentNotes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @OneToMany(mappedBy = "medicalRecord", fetch = FetchType.LAZY,
            cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH})
    List<TreatmentRecord> treatmentRecords;

    @OneToMany(mappedBy = "medicalRecord", fetch = FetchType.LAZY,
            cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH})
    List<Prescriptions> prescriptions;
}
