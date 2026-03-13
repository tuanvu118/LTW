package _2.LTW.dto.request.CareBookingRequest;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CareBookingServiceItemRequest {

    @NotNull
    Long careServiceId;
}

