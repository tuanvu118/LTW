package _2.LTW.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CareServiceResponse {

    Long id;

    String name;

    String description;

    BigDecimal price;

    Integer durationMinutes;

    Set<String> species;

    Boolean isActive;
}

