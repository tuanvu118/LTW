package _2.LTW.mapper;

import _2.LTW.dto.request.CareRecordRequest.CreateCareRecordRequest;
import _2.LTW.dto.request.CareRecordRequest.UpdateCareRecordRequest;
import _2.LTW.dto.response.CareRecordResponse.CareRecordResponse;
import _2.LTW.dto.response.CareRecordResponse.CareRecordSummaryResponse;
import _2.LTW.entity.CareRecord.CareRecord;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CareRecordMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "careBooking", ignore = true)
    @Mapping(target = "totalCost", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    CareRecord toCareRecord(CreateCareRecordRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "careBooking", ignore = true)
    @Mapping(target = "totalCost", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateCareRecord(UpdateCareRecordRequest request, @MappingTarget CareRecord careRecord);

    @Mapping(target = "careBookingId", source = "careBooking.id")
    @Mapping(target = "petId", source = "careBooking.pet.id")
    @Mapping(target = "petName", source = "careBooking.pet.name")
    @Mapping(target = "ownerId", source = "careBooking.pet.user.id")
    @Mapping(target = "ownerName", source = "careBooking.pet.user.username")
    @Mapping(target = "doctorId", source = "careBooking.doctor.id")
    @Mapping(target = "doctorName", source = "careBooking.doctor.username")
    @Mapping(target = "bookingDate", source = "careBooking.bookingDate")
    @Mapping(target = "startTime", source = "careBooking.startTime")
    @Mapping(target = "bookingStatus", source = "careBooking.status")
    CareRecordResponse toCareRecordResponse(CareRecord careRecord);

    @Mapping(target = "careBookingId", source = "careBooking.id")
    @Mapping(target = "petId", source = "careBooking.pet.id")
    @Mapping(target = "petName", source = "careBooking.pet.name")
    @Mapping(target = "doctorId", source = "careBooking.doctor.id")
    @Mapping(target = "doctorName", source = "careBooking.doctor.username")
    @Mapping(target = "bookingDate", source = "careBooking.bookingDate")
    @Mapping(target = "startTime", source = "careBooking.startTime")
    @Mapping(target = "bookingStatus", source = "careBooking.status")
    CareRecordSummaryResponse toCareRecordSummaryResponse(CareRecord careRecord);

    List<CareRecordSummaryResponse> toCareRecordSummaryResponses(List<CareRecord> careRecords);
}

