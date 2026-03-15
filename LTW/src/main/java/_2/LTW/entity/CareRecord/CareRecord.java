package _2.LTW.entity.CareRecord;

import _2.LTW.entity.CareBooking.CareBooking;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "care_records")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CareRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "care_booking_id", nullable = false, unique = true)
    CareBooking careBooking;

    @Column(name = "total_cost", precision = 10, scale = 2)
    BigDecimal totalCost;

    @Column(name = "care_notes", columnDefinition = "TEXT")
    String careNotes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;
}

