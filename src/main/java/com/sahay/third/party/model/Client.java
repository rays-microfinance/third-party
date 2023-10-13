package com.sahay.third.party.model;

import lombok.*;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Objects;

@Data
@Entity
@Table(name = "Client", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"email", "username", "phone_number"})
})
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
    @Column(name = "username", unique = true)
    private String username;
    @Column(name = "email", unique = true)
    private String email;
    @Basic
    @Column(name = "phone_number", unique = true)
    private String phoneNumber;
    @Basic
    @Column(name = "password")
    private String password;
    @Basic
    @Column(name = "created_date")
    private Timestamp createdDate;
    @Basic
    @Column(name = "is_password_changed")
    private Boolean isPasswordChanged;
    @Basic
    @Column(name = "is_active")
    private Boolean isActive = false;
    @OneToOne()
    @JoinColumn(name = "role_id", referencedColumnName = "id")
    private Role role;

    @ManyToOne()
    @JoinColumn(name = "company_id" , referencedColumnName = "id")
    private Company company;


}
