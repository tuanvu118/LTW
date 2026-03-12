package _2.LTW.mapper;

import _2.LTW.dto.request.MedicalBookingRequest.CreateMedicalBookingRequest;
import _2.LTW.dto.request.MedicalBookingRequest.CreateMedicalBookingServiceItemRequest;
import _2.LTW.dto.response.MedicalBookingResponse.MedicalBookingResponse;
import _2.LTW.dto.response.MedicalBookingResponse.MedicalBookingServiceResponse;
import _2.LTW.entity.MedicalBooking.MedicalBooking;
import _2.LTW.entity.MedicalBooking.MedicalBookingService;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalTime;
import java.util.List;

@Mapper(componentModel = "spring")
public interface MedicalBookingMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "deleteAt", ignore = true)
    @Mapping(target = "pets", ignore = true)
    @Mapping(target = "doctor", ignore = true)
    @Mapping(target = "medicalBookingsService", ignore = true)
    MedicalBooking toMedicalBooking(CreateMedicalBookingRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "timeDuration", ignore = true)
    @Mapping(target = "medicalBooking", ignore = true)
    @Mapping(target = "medicalService", ignore = true)
    MedicalBookingService toMedicalBookingService(CreateMedicalBookingServiceItemRequest request);

    @Mapping(target = "petId", source = "pets.id")
    @Mapping(target = "petName", source = "pets.name")
    @Mapping(target = "doctorId", source = "doctor.id")
    @Mapping(target = "doctorName", source = "doctor.username")
    @Mapping(target = "estimatedEndTime", expression = "java(toEstimatedEndTime(booking))")
    @Mapping(target = "totalDuration", expression = "java(toTotalDuration(booking))")
    @Mapping(target = "services", source = "medicalBookingsService")
    MedicalBookingResponse toMedicalBookingResponse(MedicalBooking booking);

    List<MedicalBookingResponse> toMedicalBookingResponses(List<MedicalBooking> bookings);

    @Mapping(target = "medicalServiceId", source = "medicalService.id")
    @Mapping(target = "medicalServiceName", source = "medicalService.name")
    MedicalBookingServiceResponse toMedicalBookingServiceResponse(MedicalBookingService detail);

    List<MedicalBookingServiceResponse> toMedicalBookingServiceResponses(List<MedicalBookingService> details);

    default Integer toTotalDuration(MedicalBooking booking) {
        if (booking.getMedicalBookingsService() == null) {
            return 0;
        }

        return booking.getMedicalBookingsService().stream()
                .map(MedicalBookingService::getTimeDuration)
                .reduce(0, Integer::sum);
    }

    default LocalTime toEstimatedEndTime(MedicalBooking booking) {
        return booking.getStartTime().plusMinutes(toTotalDuration(booking));
    }
}
