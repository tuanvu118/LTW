package com.BTL_JAVA.BTL.Controller.User;

import com.BTL_JAVA.BTL.DTO.Request.ApiResponse;
import com.BTL_JAVA.BTL.DTO.Request.User.UserCreationRequest;
import com.BTL_JAVA.BTL.DTO.Request.User.UserUpdateRequest;
import com.BTL_JAVA.BTL.DTO.Response.Security.PermissionResponse;
import com.BTL_JAVA.BTL.DTO.Response.Security.RoleResponse;
import com.BTL_JAVA.BTL.DTO.Response.User.UserResponse;
import com.BTL_JAVA.BTL.Entity.Permission;
import com.BTL_JAVA.BTL.Entity.Role;
import com.BTL_JAVA.BTL.Entity.User;
import com.BTL_JAVA.BTL.Service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor

public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ApiResponse<User> createUser(@ModelAttribute @Valid UserCreationRequest request) throws IOException {
        ApiResponse<User> response = new ApiResponse<>();

        response.setResult(userService.createUser(request));

        return response;
    }
    @GetMapping()
     List<UserResponse> getAllUsers() {
        List<User> users = userService.getAllUsers();

        List<UserResponse> result = new ArrayList<>();

        for (User u : users) {
            UserResponse ur = new UserResponse();
            ur.setId(u.getId());
            ur.setUserName(u.getFullName());
            ur.setEmail(u.getEmail());
            ur.setPhoneNumber(u.getPhoneNumber());
            ur.setAvatar(u.getAvatar());

            Set<RoleResponse> roleDtos = new LinkedHashSet<>();
            Set<Role> roles = (u.getRoles() != null) ? u.getRoles() : java.util.Set.of();
            for (Role r : roles) {
                RoleResponse rr = new RoleResponse();
                rr.setName(r.getNameRoles());
                rr.setDescription(r.getDescription());
                Set<PermissionResponse> permDtos = new LinkedHashSet<>();
                Set<Permission> perms = (r.getPermissions() != null) ? r.getPermissions() : java.util.Set.of();

                for (Permission p : perms) {
                    PermissionResponse pr = new PermissionResponse();
                    pr.setName(p.getNamePermission());     // đổi đúng tên getter của bạn
                    pr.setDescription(p.getDescription());
                    permDtos.add(pr);
                }
                rr.setPermissions(permDtos);
                roleDtos.add(rr);
            }
            ur.setRoles(roleDtos);
            result.add(ur);
        }

        return result;
    }

    @GetMapping("/{userId}")
    UserResponse getUser(@PathVariable("userId") int userId){
       return userService.getUser(userId);
    }

    @GetMapping("/myInfor")
    UserResponse getMyinfor(){
        return userService.getMyInfo();
    }

    @PutMapping(value = "/{userId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserResponse updateUser(
            @PathVariable("userId") int userId,
            @ModelAttribute UserUpdateRequest request
    ) throws IOException {
        return userService.updateUser(userId, request);
    }


    @DeleteMapping("/{userId}")
    public String deleteUser(@PathVariable("userId") int userId){
          userService.deleteUser(userId);
          return "User Deleted Successfully";
    }
}
