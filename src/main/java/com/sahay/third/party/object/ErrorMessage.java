package com.sahay.third.party.object;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ErrorMessage {

    private String response;
    private String responseMessage;
    private String path;
    private LocalDateTime date;
}
