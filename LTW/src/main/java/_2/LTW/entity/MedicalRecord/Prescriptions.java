package _2.LTW.entity.MedicalRecord;

import _2.LTW.entity.Medicine;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Entity
@Table(name = "prescriptions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Prescriptions {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medical_record_id", nullable = false)
    MedicalRecords medicalRecord;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicine_id", nullable = false)
    Medicine medicine;

    @Column(name = "quantity", nullable = false)
    Integer quantity;

    @Column(name = "price", precision = 10, scale = 2, nullable = false)
    BigDecimal price;

    @Column(name = "dosage", length = 100)
    String dosage;

    @Column(name = "note", columnDefinition = "TEXT")
    String note;
}
