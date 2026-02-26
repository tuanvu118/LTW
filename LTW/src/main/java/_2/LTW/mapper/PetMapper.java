package _2.LTW.mapper;


import _2.LTW.dto.request.PetCreateRequest;
import _2.LTW.dto.request.PetUpdateRequest;
import _2.LTW.dto.response.PetResponse;
import _2.LTW.entity.Pets.Pets;
import _2.LTW.entity.User;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface PetMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "created_at", ignore = true)
    @Mapping(target = "delete_at", ignore = true)
    @Mapping(target = "user", source = "owner")
    @Mapping(target = "img_url", ignore = true)
    Pets toEntity(PetCreateRequest request, User owner);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "created_at", ignore = true)
    @Mapping(target = "delete_at", ignore = true)
    @Mapping(target = "img_url", ignore = true)
    @Mapping(target = "user", ignore = true)
    void updateEntity(@MappingTarget Pets pets, PetUpdateRequest request);

    @Mapping(target = "ownerId", source = "user.id")
    @Mapping(target = "ownerName", source = "user.username")
    PetResponse toResponse(Pets pets);
}
