package com.klix.backend.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
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
        @UniqueConstraint(columnNames =  {"client_id", "person_id"})
    }
)
public class ClientPerson
{
	@Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @NotNull
    @MapsId("id")
    private Person person;

    @ManyToOne
    @NotNull
    @MapsId("id")
    private Client client;
}