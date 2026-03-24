package com.BTL_JAVA.BTL.DTO.Request.User;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AddressRequest {

    String street;

    String ward;

    String city;
    
    boolean defaultAddress;
}