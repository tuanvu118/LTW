package _2.LTW.mapper;

import _2.LTW.dto.request.care_booking.CareBookingCreateRequest;
import _2.LTW.dto.request.care_booking.CareBookingUpdateRequest;
import _2.LTW.dto.response.CareBookingResponse;
import _2.LTW.entity.CareBooking;
import _2.LTW.entity.Pets.Pets;
import _2.LTW.entity.User;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", uses = {PetMapper.class, UserMapper.class})
public interface CareBookingMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "pet", source = "pet")
    @Mapping(target = "doctor", source = "doctor")
    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "deleteAt", ignore = true)
    CareBooking toEntity(CareBookingCreateRequest request, Pets pet, User doctor);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "pet", ignore = true)
    @Mapping(target = "doctor", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "deleteAt", ignore = true)
    void updateEntity(@MappingTarget CareBooking careBooking, CareBookingUpdateRequest request);

    @Mapping(target = "pet", source = "pet")
    @Mapping(target = "doctor", source = "doctor")
    @Mapping(target = "status", expression = "java(careBooking.getStatus().name())")
    CareBookingResponse toResponse(CareBooking careBooking);

    List<CareBookingResponse> toResponseList(List<CareBooking> careBookings);
}

