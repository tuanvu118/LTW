package _2.LTW.entity;

import _2.LTW.entity.MedicalRecord.MedicalRecords;
import _2.LTW.entity.MedicalRecord.TreatmentRecord;
import lombok.*;
import jakarta.persistence.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "treatment_methods")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TreatmentMethods {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "name", nullable = false, unique = true)
    String name;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    BigDecimal price;

    @OneToMany(mappedBy = "treatmentMethods", fetch = FetchType.LAZY,
            cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH})
    List<TreatmentRecord> treatmentRecords;
}
