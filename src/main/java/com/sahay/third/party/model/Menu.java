package com.sahay.third.party.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "Menu")
@Entity
public class Menu {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String name;
    private String description;
    @Column(name = "created_at")
    private Timestamp createdAt;
    @JsonIgnore
    @OneToMany(cascade = CascadeType.ALL , mappedBy = "menu")
    private List<Permission> permissions;

}
