package _2.LTW.mapper;

import _2.LTW.dto.request.care_service.CareServiceCreateRequest;
import _2.LTW.dto.request.care_service.CareServiceUpdateRequest;
import _2.LTW.dto.response.CareServiceResponse;
import _2.LTW.entity.CareService;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CareServiceMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isActive", constant = "true")
    @Mapping(target = "petType", expression = "java(mapPetType(request.getPetType()))")
    CareService toEntity(CareServiceCreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "petType", expression = "java(request.getPetType() != null ? mapPetType(request.getPetType()) : careService.getPetType())")
    void updateEntity(@MappingTarget CareService careService, CareServiceUpdateRequest request);

    @Mapping(target = "petType", expression = "java(careService.getPetType() != null ? careService.getPetType().name() : null)")
    CareServiceResponse toResponse(CareService careService);

    List<CareServiceResponse> toResponseList(List<CareService> careServices);

    default CareService.PetType mapPetType(String petType) {
        if (petType == null || petType.isBlank()) {
            return null;
        }
        try {
            return CareService.PetType.valueOf(petType.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

