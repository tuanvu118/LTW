package _2.LTW.dto.request;

import lombok.*;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class RoleRequest {
    private Long userId;
    private Integer roleId;
}
