package com.sahay.third.party.model;

import lombok.*;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Objects;

@Data
@Entity
@Table(name = "Client")
public class Client {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Basic
    @Column(name = "client_name")
    private String clientName;
    @Basic
    @Column(name = "client_req_type")
    private String clientReqType;
    @Basic
    @Column(name = "client_account")
    private String clientAccount;
    @Basic
    @Column(name = "type")
    private String type;
    @Basic
    @Column(name = "not_url")
    private String notUrl;
    @Basic
    @Column(name = "username")
    private String username;
    @Basic
    @Column(name = "password")
    private String password;
    @Basic
    @Column(name = "created_date")
    private Timestamp createdDate;

}
