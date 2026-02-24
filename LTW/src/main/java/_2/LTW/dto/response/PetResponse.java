package _2.LTW.dto.response;

import _2.LTW.entity.Pets.PetGender;
import _2.LTW.entity.Pets.PetSpecies;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PetResponse {
    Integer id;
    String name;
    PetSpecies species;
    PetGender gender;
    String breed;
    Integer age;
    BigDecimal weight;
    String medical_history;
    String img_url;
    LocalDateTime created_at;

    Long ownerId;
    String ownerName;
}
