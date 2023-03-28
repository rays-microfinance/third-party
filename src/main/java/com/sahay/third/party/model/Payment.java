package com.sahay.third.party.model;

import lombok.*;

import javax.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Objects;

@Data
@Entity
@Table(name = "Payment")
public class Payment {
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
    @Column(name = "account_number")
    private String accountNumber;
    @Basic
    @Column(name = "amount")
    private BigDecimal amount;
    @Basic
    @Column(name = "validation_code")
    private String validationCode;
    @Basic
    @Column(name = "validation_expiry_date")
    private Timestamp validationExpiryDate;
    @Basic
    @Column(name = "transaction_status")
    private Integer transactionStatus;
    @Basic
    @Column(name = "request_payload")
    private String requestPayload;
    @Basic
    @Column(name = "request_date")
    private Timestamp requestDate;
    @Basic
    @Column(name = "transaction_cost")
    private BigDecimal transactionCost;
    @Basic
    @Column(name = "customer_charge")
    private BigDecimal customerCharge;
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
