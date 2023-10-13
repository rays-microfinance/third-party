package com.sahay.third.party.object;


import com.sahay.third.party.exception.PasswordMatches;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@Data
@AllArgsConstructor
@NoArgsConstructor
@PasswordMatches
public class ChangePasswordRequest {

    @NotBlank
    private String password;
    @NotBlank
    private String repeatPassword;
}
