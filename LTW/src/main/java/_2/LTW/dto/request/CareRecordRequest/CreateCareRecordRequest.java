package _2.LTW.dto.request.CareRecordRequest;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateCareRecordRequest {

    @NotNull
    Long careBookingId;

    String careNotes;
}

