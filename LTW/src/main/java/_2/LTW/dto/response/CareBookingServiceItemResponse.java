package _2.LTW.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CareBookingServiceItemResponse {

    Long id;
    Long careServiceId;
    String careServiceName;
    BigDecimal price;
    Integer durationMinutes;
}

