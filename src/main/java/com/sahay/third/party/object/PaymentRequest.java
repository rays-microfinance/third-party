package com.sahay.third.party.object;

import lombok.Data;

@Data
public class PaymentRequest {
    public String PhoneNumber;
    public String Pin;
    public String TransactionRef;
    public String Amount;
}
