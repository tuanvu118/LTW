package _2.LTW.entity;

import lombok.*;
import jakarta.persistence.*;
import lombok.experimental.FieldDefaults;
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

    @Column(name = "price", nullable = false)
    Integer price;
    
}
