package _2.LTW.mapper;

import _2.LTW.dto.request.TreatmentMethodsRequest;
import _2.LTW.dto.response.TreatmentMethodsResponse;
import _2.LTW.entity.TreatmentMethods;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TreatmentMethodsMapper {

    TreatmentMethods toEntity(TreatmentMethodsRequest request);
    TreatmentMethodsResponse toResponse(TreatmentMethods treatmentMethods);
    List<TreatmentMethodsResponse> toResponses(List<TreatmentMethods> treatmentMethods);
}
