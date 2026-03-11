package _2.LTW.entity.MedicalRecord;

import _2.LTW.entity.TreatmentMethods;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

import java.math.BigDecimal;

@Entity
@Table(name = "treatment_record")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TreatmentRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "price", precision = 10, scale = 2, nullable = false)
    BigDecimal price;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "treatment_methods_id")
    TreatmentMethods treatmentMethods;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medical_record_id")
    MedicalRecords medicalRecord;

}
