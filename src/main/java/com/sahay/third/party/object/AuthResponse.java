package com.sahay.third.party.object;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {

    private String response;
    private String responseDescription;
    private String username;
    private String token;
}
