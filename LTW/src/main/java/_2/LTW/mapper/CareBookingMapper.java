package _2.LTW.mapper;

import _2.LTW.dto.request.CareBookingRequest.CareBookingCreateRequest;
import _2.LTW.dto.request.CareBookingRequest.CareBookingServiceItemRequest;
import _2.LTW.dto.response.CareBookingResponse;
import _2.LTW.dto.response.CareBookingServiceItemResponse;
import _2.LTW.entity.CareBooking.CareBooking;
import _2.LTW.entity.CareBooking.CareBookingServiceItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalTime;
import java.util.List;

@Mapper(componentModel = "spring")
public interface CareBookingMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "pet", ignore = true)
    @Mapping(target = "doctor", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "deleteAt", ignore = true)
    @Mapping(target = "careBookingServices", ignore = true)
    CareBooking toCareBooking(CareBookingCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "price", ignore = true)
    @Mapping(target = "durationMinutes", ignore = true)
    @Mapping(target = "careBooking", ignore = true)
    @Mapping(target = "careService", ignore = true)
    CareBookingServiceItem toCareBookingServiceItem(CareBookingServiceItemRequest request);

    @Mapping(target = "petId", source = "pet.id")
    @Mapping(target = "petName", source = "pet.name")
    @Mapping(target = "doctorId", source = "doctor.id")
    @Mapping(target = "doctorName", source = "doctor.username")
    @Mapping(target = "estimatedEndTime", expression = "java(toEstimatedEndTime(booking))")
    @Mapping(target = "totalDuration", expression = "java(toTotalDuration(booking))")
    @Mapping(target = "services", source = "careBookingServices")
    CareBookingResponse toResponse(CareBooking booking);

    List<CareBookingResponse> toResponses(List<CareBooking> bookings);

    @Mapping(target = "careServiceId", source = "careService.id")
    @Mapping(target = "careServiceName", source = "careService.name")
    CareBookingServiceItemResponse toServiceItemResponse(CareBookingServiceItem item);

    List<CareBookingServiceItemResponse> toServiceItemResponses(List<CareBookingServiceItem> items);

    default Integer toTotalDuration(CareBooking booking) {
        if (booking.getCareBookingServices() == null) {
            return 0;
        }
        return booking.getCareBookingServices().stream()
                .map(CareBookingServiceItem::getDurationMinutes)
                .reduce(0, Integer::sum);
    }

    default LocalTime toEstimatedEndTime(CareBooking booking) {
        return booking.getStartTime().plusMinutes(toTotalDuration(booking));
    }
}

