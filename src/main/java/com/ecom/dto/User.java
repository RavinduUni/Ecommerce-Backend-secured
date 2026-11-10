package com.ecom.dto;

import com.ecom.enums.UserRole;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class User {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;

    /** Accepted in requests (register, login) but never written to responses. */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    private String phone;
    /** No default here: a default would overwrite the stored role on every partial update. */
    private UserRole role;
    private Address address;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
