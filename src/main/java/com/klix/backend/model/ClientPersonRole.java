package com.klix.backend.model;

import javax.persistence.*;

import javax.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;



/**
 * 
 */
@Getter
@Setter
@Entity
@Table(
    uniqueConstraints = {
        @UniqueConstraint(columnNames =  {"client_person_id", "client_role_id"})
    }
)
public class ClientPersonRole
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @NotNull
    private ClientRole clientRole;

    @ManyToOne
    @NotNull
    private ClientPerson clientPerson;
}