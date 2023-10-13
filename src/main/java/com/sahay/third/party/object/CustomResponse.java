package com.sahay.third.party.object;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.annotation.security.DenyAll;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomResponse {

    private String response;
    private String responseDescription;
}
