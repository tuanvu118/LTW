package _2.LTW.mapper;

import _2.LTW.dto.request.doctor_work.SlotRequest;
import _2.LTW.dto.response.doctor_work.SlotResponse;
import _2.LTW.dto.response.doctor_work.WeeklyScheduleResponse;
import _2.LTW.entity.DoctorWork;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DoctorWorkMapper {

    DoctorWork toDoctorWork(SlotRequest request);

    SlotResponse toSlotResponse(DoctorWork doctorWork);

    List<SlotResponse> toSlotResponses(List<DoctorWork> doctorWorks);

}
