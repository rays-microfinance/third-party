package com.sahay.third.party.object;

import lombok.Data;

@Data
public class PaymentValidation {
    public String ValidationCode;
    public String TransactionRef;
    public String StagingRef;
}
