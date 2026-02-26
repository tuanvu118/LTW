package _2.LTW.dto.request;

import _2.LTW.entity.Pets.PetGender;
import _2.LTW.entity.Pets.PetSpecies;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PetUpdateRequest {
    @NotBlank
    String name;

    @NotNull
    PetSpecies species;

    @NotNull
    PetGender gender;

    @NotBlank
    String breed;

    @NotNull
    @Min(0)
    Integer age;

    @DecimalMin("0.00")
    BigDecimal weight;

    String medical_history;
    MultipartFile img_url;
}
