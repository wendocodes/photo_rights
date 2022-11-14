package com.klix.backend.model;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.FetchType;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;


/**
 * Model for Groups
 */
@Getter
@Setter
@Entity
public class Groups {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String groupName;

    //Eager Mode otherwise problems with the table view for groups therefore fetch it with the rest of the fields (eagerly)    
    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<Person> supervisors;

    @ManyToOne
    @NotNull
    @MapsId("id")
    private Client client;
    
    //Eager Mode otherwise problems with the table view for groups therefore fetch it with the rest of the fields (eagerly)
    @ManyToMany(fetch = FetchType.EAGER, cascade=CascadeType.ALL)
    private Set<Person> members;

    @ManyToMany(fetch = FetchType.EAGER, cascade=CascadeType.ALL)
    private Set<Event> events;


    /**
     * Utillity Methode um Events zu hinzuzufügen.
     */
    public void addEvent(Event event)
    {
        if (this.events == null)
        {
            this.events = new HashSet<>();
        }

        this.events.add(event);
    }


    /**
     * Utillity Methode um Events zu entfernen.
     */
    public void removeEvent(Event event)
    {
        this.events.remove(event);
    }

}
