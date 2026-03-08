package _2.LTW.mapper;

import _2.LTW.dto.request.MedicineRequest;
import _2.LTW.dto.response.MedicineResponse;
import _2.LTW.entity.Medicine;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MedicineMapper {

    @Mapping(target = "id", ignore = true)
    Medicine toMedicine(MedicineRequest request);

    MedicineResponse toMedicineResponse(Medicine medicine);

    List<MedicineResponse> toMedicineResponses(List<Medicine> medicines);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    void updateMedicineFromRequest(MedicineRequest request, @MappingTarget Medicine medicine);
}

