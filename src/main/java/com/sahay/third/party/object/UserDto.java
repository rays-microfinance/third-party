package com.sahay.third.party.object;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDto {

    private String email;
    private String phoneNumber;
    private String password;
    private String userName;
    private int roleId;
}
