package com.sahay.third.party.object;


import com.sahay.third.party.model.Company;
import com.sahay.third.party.model.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateUserRequest {

    @NotBlank
    private String username;
    @NotBlank
    @Email(message = "Invalid email")
    private String email;
    @NotBlank
    private String phoneNumber;
//    @NotBlank
    private Role role;

    private Company company;
}
