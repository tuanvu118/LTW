package _2.LTW.mapper;

import _2.LTW.dto.response.UserResponse;
import _2.LTW.entity.User;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toUserResponse(User user);

    List<UserResponse> toUserResponses(List<User> users);
}

