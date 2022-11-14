package com.klix.backend.model;

import javax.persistence.*;

import lombok.Getter;
import lombok.Setter;



/**
 * 
 */
@Getter
@Setter
@Entity
public class ClientRole
{

    public ClientRole() {}
    
    public ClientRole(Long client_role_id, String client_role_name) {
        this.id = client_role_id;
        this.name = client_role_name;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String name;
}