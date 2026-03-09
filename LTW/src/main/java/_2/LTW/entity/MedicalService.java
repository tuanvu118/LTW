package _2.LTW.entity;

import _2.LTW.entity.MedicalBooking.MedicalBookingService;
import jakarta.persistence.*;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Entity
@Table(name = "medical_services")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MedicalService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "name", unique = true, nullable = false)
    String name;

    @Column(name = "time_duration", nullable = false)
    Integer timeDuration;

    @Column(name = "description")
    String description;

    @OneToMany(mappedBy = "medicalService", fetch = FetchType.LAZY,
            cascade = {CascadeType.DETACH,CascadeType.MERGE,CascadeType.PERSIST, CascadeType.REFRESH})
    List<MedicalBookingService>  medicalBookingServices;

}
