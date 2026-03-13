package _2.LTW.mapper;

import _2.LTW.dto.request.CareServiceRequest.CareServiceCreateRequest;
import _2.LTW.dto.request.CareServiceRequest.CareServiceUpdateRequest;
import _2.LTW.dto.response.CareServiceResponse;
import _2.LTW.entity.CareService;
import org.mapstruct.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface CareServiceMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isActive", constant = "true")
    @Mapping(target = "petTypes", expression = "java(mapPetTypes(request.getPetTypes()))")
    CareService toEntity(CareServiceCreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "petTypes", expression = "java(request.getPetTypes() != null ? mapPetTypes(request.getPetTypes()) : careService.getPetTypes())")
    void updateEntity(@MappingTarget CareService careService, CareServiceUpdateRequest request);

    @Mapping(target = "petTypes", expression = "java(mapPetTypeNames(careService.getPetTypes()))")
    CareServiceResponse toResponse(CareService careService);

    List<CareServiceResponse> toResponseList(List<CareService> careServices);

    default Set<CareService.PetType> mapPetTypes(Set<String> petTypes) {
        if (petTypes == null || petTypes.isEmpty()) {
            return null;
        }

        return petTypes.stream()
                .filter(petType -> petType != null && !petType.isBlank())
                .map(petType -> CareService.PetType.valueOf(petType.toUpperCase()))
                .collect(Collectors.toSet());
    }

    default Set<String> mapPetTypeNames(Set<CareService.PetType> petTypes) {
        if (petTypes == null || petTypes.isEmpty()) {
            return null;
        }

        return petTypes.stream()
                .map(Enum::name)
                .collect(Collectors.toSet());
    }
}

