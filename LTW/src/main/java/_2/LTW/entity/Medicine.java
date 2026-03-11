package _2.LTW.entity;

import _2.LTW.entity.MedicalRecord.Prescriptions;
import _2.LTW.entity.MedicalRecord.TreatmentRecord;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@Entity
@Table(name = "medicines")
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Medicine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "usage_instruction", nullable = true)
    private String usageInstruction;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @OneToMany(mappedBy = "medicine", fetch = FetchType.LAZY,
            cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH})
    List<Prescriptions> prescriptions;
}
