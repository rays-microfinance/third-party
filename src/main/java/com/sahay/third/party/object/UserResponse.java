package com.sahay.third.party.object;

import com.sahay.third.party.model.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.sql.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse {

    private Integer id;
    private String username;
    private String email;
    private Role role;
    private Boolean isPasswordChanged;
    private Boolean isActive;
    private Timestamp createdDate;



}
