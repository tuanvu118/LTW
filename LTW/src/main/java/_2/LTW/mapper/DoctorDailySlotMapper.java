package _2.LTW.mapper;

import _2.LTW.dto.response.DoctorDailySlotResponse.AvailableSlotReponse;
import _2.LTW.dto.response.DoctorDailySlotResponse.DoctorSlotResponse;
import _2.LTW.entity.DoctorDailySlot.DoctorDailySlot;
import _2.LTW.entity.User;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DoctorDailySlotMapper {

    DoctorSlotResponse toDoctorSlotResponse(User doctor);

    AvailableSlotReponse toAvailableSlotResponse(DoctorDailySlot slot);

    List<AvailableSlotReponse> toAtoAvailableSlotsResponse(List<DoctorDailySlot> slots);

}
