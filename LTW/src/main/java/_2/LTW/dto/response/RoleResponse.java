package _2.LTW.dto.response;

import lombok.*;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class RoleResponse {
    private Integer id;
    private String name;
    private String description;
}
