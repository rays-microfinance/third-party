package com.sahay.third.party.model;

import lombok.Data;

import javax.persistence.*;
import java.sql.Timestamp;

@Data
@Entity
@Table(name = "BCTransaction")
public class PaymentTransaction {
    @Id
    @Column(name = "TransactionId")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long transactionId;
    @Basic
    @Column(name = "CompleteTransaction")
    private boolean completeTransaction;
    @Basic
    @Column(name = "BankCode")
    private String bankCode;
    @Basic
    @Column(name = "TxnStatus")
    private Integer txnStatus;
    @Basic
    @Column(name = "Reference")
    private String reference;
    @Basic
    @Column(name = "ServiceCode")
    private String serviceCode;
    @Basic
    @Column(name = "MessageType")
    private String messageType;
    @Basic
    @Column(name = "TransactionType")
    private String transactionType;
    @Basic
    @Column(name = "MSISDN")
    private String msisdn;
    @Basic
    @Column(name = "CreditAccount")
    private String creditAccount;
    @Basic
    @Column(name = "DebitAccount")
    private String debitAccount;
    @Basic
    @Column(name = "Currency")
    private String currency;
    @Basic
    @Column(name = "Amount")
    private String amount;
    @Basic
    @Column(name = "InMessage")
    private String inMessage;
    @Basic
    @Column(name = "AgencyMessage")
    private String agencyMessage;
    @Basic
    @Column(name = "AgencyResponse")
    private String agencyResponse;
    @Basic
    @Column(name = "AgencyResponseDescription")
    private String agencyResponseDescription;
    @Basic
    @Column(name = "AgencyTransactionNumber")
    private String agencyTransactionNumber;
    @Basic
    @Column(name = "Response")
    private String response;
    @Basic
    @Column(name = "ResponseDescription")
    private String responseDescription;
    @Basic
    @Column(name = "Result")
    private String result;
    @Basic
    @Column(name = "ValidationCode")
    private String validationCode;
    @Basic
    @Column(name = "Channel")
    private String channel;
    @Basic
    @Column(name = "DateCreated")
    private Timestamp dateCreated;
    @Basic
    @Column(name = "IsReversed")
    private long isReversed;
}
