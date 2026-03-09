package _2.LTW.entity;

import jakarta.persistence.*;
import lombok.*;

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

    @Column(name = "price", nullable = false)
    private Double price;
}
