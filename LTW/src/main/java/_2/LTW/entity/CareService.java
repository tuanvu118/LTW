package _2.LTW.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.Set;

@Entity
@Table(name = "care_services")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CareService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "name", nullable = false, length = 100)
    String name;

    @Column(name = "description", columnDefinition = "TEXT")
    String description;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    BigDecimal price;

    @Column(name = "duration_minutes", nullable = false)
    Integer durationMinutes;

    @ElementCollection(targetClass = PetType.class)
    @CollectionTable(name = "care_service_pet_types", joinColumns = @JoinColumn(name = "care_service_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "pet_type")
    Set<PetType> petTypes;

    @Column(name = "is_active")
    @Builder.Default
    Boolean isActive = true;

    public enum PetType {
        DOG,
        CAT
    }
}

