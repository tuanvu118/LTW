package _2.LTW.mapper;

import _2.LTW.dto.request.DoctorWork.SlotRequest;
import _2.LTW.dto.response.DoctorWork.SlotResponse;
import _2.LTW.entity.DoctorWork.DoctorWork;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DoctorWorkMapper {

    DoctorWork toDoctorWork(SlotRequest request);

    SlotResponse toSlotResponse(DoctorWork doctorWork);

    List<SlotResponse> toSlotResponses(List<DoctorWork> doctorWorks);

}
