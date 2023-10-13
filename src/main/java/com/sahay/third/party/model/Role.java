package com.sahay.third.party.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import java.sql.Timestamp;



@Data
@Entity
@Table(name = "Role")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String name;
    @Column(name = "role_code")
    private String roleCode;
    @Column(name = "created_at")
    private Timestamp createdAt;
    private boolean active = true;
//    @JsonIgnore
//    @OneToOne(mappedBy = "role" , fetch = FetchType.EAGER)
//    private Client client;
}
