package _2.LTW.mapper;

import _2.LTW.dto.request.MedicalServiceRequest;
import _2.LTW.dto.response.MedicalServiceResponse;
import _2.LTW.entity.MedicalService;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MedicalServiceMapper {
    MedicalService toMedicalService(MedicalServiceRequest request);

    MedicalServiceResponse toMedicalServiceResponse(MedicalService medicalService);

    List<MedicalServiceResponse> toMedicalServiceResponses(List<MedicalService> medicalServices);

    void updateMedicalService(@MappingTarget MedicalService medicalService, MedicalServiceRequest request);
}
