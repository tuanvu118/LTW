package _2.LTW.entity.Pets;

import _2.LTW.entity.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "pets")
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Pets {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    @Column(name = "name",  nullable = false)
    String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "species", nullable = false)
    PetSpecies species;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false)
    PetGender gender;

    @Column(name = "breed", nullable = false)
    String breed;

    @Column(name = "age", nullable = false)
    Integer age;

    @Column(name = "weight",precision =  5, scale = 2)
    BigDecimal weight;

    @Column(name = "medical_history")
    String medical_history;

    @Column(name = "img_url")
    String img_url;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    LocalDateTime created_at;

    @Column(name = "delete_at")
    LocalDateTime delete_at;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    User user;
}
