package com.sahay.third.party.object;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FloatTransferRequest {

    private Double amount;
    private String transactionType;
    private String fromAccount;
    private String toAccount;

}
