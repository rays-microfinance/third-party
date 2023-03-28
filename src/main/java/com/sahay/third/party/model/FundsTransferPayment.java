package com.sahay.third.party.model;

import lombok.*;

import javax.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Objects;

@Data
@Entity
@Table(name = "FundsTransferPayment")
public class FundsTransferPayment {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Basic
    @Column(name = "client")
    private String client;
    @Basic
    @Column(name = "trans_ref")
    private String transRef;
    @Basic
    @Column(name = "phone_number")
    private String phoneNumber;
    @Basic
    @Column(name = "biller_reference")
    private String billerReference;
    @Basic
    @Column(name = "account_type")
    private String accountType;
    @Basic
    @Column(name = "account_number")
    private String accountNumber;
    @Basic
    @Column(name = "amount")
    private BigDecimal amount;
    @Basic
    @Column(name = "status")
    private int status;
    @Basic
    @Column(name = "created_date")
    private Timestamp createdDate;
    @Basic
    @Column(name = "transaction_post_payload")
    private String transactionPostPayload;
    @Basic
    @Column(name = "transaction_status")
    private Integer transactionStatus;
    @Basic
    @Column(name = "third_party_payload")
    private String thirdPartyPayload;
    @Basic
    @Column(name = "response_staus")
    private String responseStaus;
    @Basic
    @Column(name = "response_description")
    private String responseDescription;
    @Basic
    @Column(name = "response_payload")
    private String responsePayload;
    @Basic
    @Column(name = "processed_datetime")
    private Timestamp processedDatetime;

}
