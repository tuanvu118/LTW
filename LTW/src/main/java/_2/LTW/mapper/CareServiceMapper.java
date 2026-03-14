package _2.LTW.mapper;

import _2.LTW.dto.request.CareServiceRequest.CareServiceCreateRequest;
import _2.LTW.dto.request.CareServiceRequest.CareServiceUpdateRequest;
import _2.LTW.dto.response.CareServiceResponse;
import _2.LTW.entity.CareService;
import org.mapstruct.*;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface CareServiceMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isActive", constant = "true")
    @Mapping(target = "species", expression = "java(toSpeciesColumn(request.getSpecies()))")
    CareService toEntity(CareServiceCreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "species", expression = "java(request.getSpecies() != null ? toSpeciesColumn(request.getSpecies()) : careService.getSpecies())")
    void updateEntity(@MappingTarget CareService careService, CareServiceUpdateRequest request);

    @Mapping(target = "species", expression = "java(toSpeciesSet(careService.getSpecies()))")
    CareServiceResponse toResponse(CareService careService);

    List<CareServiceResponse> toResponseList(List<CareService> careServices);

    default String toSpeciesColumn(Set<String> species) {
        if (species == null || species.isEmpty()) {
            return null;
        }

        return species.stream()
                .filter(item -> item != null && !item.isBlank())
                .map(CareServiceMapper::normalizeSpeciesToken)
                .distinct()
                .sorted()
                .collect(Collectors.joining(","));
    }

    default Set<String> toSpeciesSet(String species) {
        if (species == null || species.isBlank()) {
            return null;
        }

        return Arrays.stream(species.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .collect(Collectors.toSet());
    }

    private static String normalizeSpeciesToken(String raw) {
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "dog" -> "Dog";
            case "cat" -> "Cat";
            default -> throw new IllegalArgumentException("Species không hợp lệ: " + raw);
        };
    }
}

