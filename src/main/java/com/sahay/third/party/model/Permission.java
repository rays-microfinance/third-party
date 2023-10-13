package com.sahay.third.party.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "Permission")
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String name;
    private String description;
    @ManyToOne
    @JoinColumn(name = "menu_id")
//    @JsonIgnore
    private Menu menu;
    @Column(name = "allowed_role_code")
    private String allowedRoleCode;
    @Column(name = "show_on_sidebar")
    private boolean showOnSideBar;
    @Column(name = "frontend_url")
    private String frontEndUrl;
    @Column(name = "created_At")
    private Timestamp createdAt;

}
