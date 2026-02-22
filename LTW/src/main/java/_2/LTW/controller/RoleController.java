package _2.LTW.controller;

import lombok.*;
import lombok.experimental.FieldDefaults;
import _2.LTW.service.RoleService;
import _2.LTW.dto.response.RoleResponse;
import _2.LTW.dto.request.RoleRequest;
import _2.LTW.dto.response.MessageResponse;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/roles")

public class RoleController {
    private final RoleService roleService;

    @GetMapping
    public ResponseEntity<List<RoleResponse>> getAllRole() {
        return ResponseEntity.ok(roleService.getAllRole());
    }

    @PostMapping
    public ResponseEntity<MessageResponse> addRoleToUser(@RequestBody RoleRequest roleRequest) {
        return ResponseEntity.ok(roleService.addRoleToUser(roleRequest));
    }

    @DeleteMapping
    public ResponseEntity<MessageResponse> deleteRoleFromUser(@RequestBody RoleRequest roleRequest) {
        return ResponseEntity.ok(roleService.deleteRoleFromUser(roleRequest));
    }
    
}
