package _2.LTW.mapper;

import _2.LTW.dto.request.CareServiceRequest.CareServiceCreateRequest;
import _2.LTW.dto.request.CareServiceRequest.CareServiceUpdateRequest;
import _2.LTW.dto.response.CareServiceResponse;
import _2.LTW.entity.CareService;
import _2.LTW.entity.Pets.PetSpecies;
import org.mapstruct.*;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface CareServiceMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isActive", constant = "true")
    @Mapping(target = "species", expression = "java(mapSpecies(request.getSpecies()))")
    CareService toEntity(CareServiceCreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "species", expression = "java(request.getSpecies() != null ? mapSpecies(request.getSpecies()) : careService.getSpecies())")
    void updateEntity(@MappingTarget CareService careService, CareServiceUpdateRequest request);

    @Mapping(target = "species", expression = "java(mapSpeciesNames(careService.getSpecies()))")
    CareServiceResponse toResponse(CareService careService);

    List<CareServiceResponse> toResponseList(List<CareService> careServices);

    default Set<PetSpecies> mapSpecies(Set<String> species) {
        if (species == null || species.isEmpty()) {
            return null;
        }

        return species.stream()
                .filter(item -> item != null && !item.isBlank())
                .map(this::toPetSpecies)
                .collect(Collectors.toSet());
    }

    default Set<String> mapSpeciesNames(Set<PetSpecies> species) {
        if (species == null || species.isEmpty()) {
            return null;
        }

        return species.stream()
                .map(Enum::name)
                .collect(Collectors.toSet());
    }

    default PetSpecies toPetSpecies(String raw) {
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "dog" -> PetSpecies.Dog;
            case "cat" -> PetSpecies.Cat;
            default -> throw new IllegalArgumentException("Species không hợp lệ: " + raw);
        };
    }
}

