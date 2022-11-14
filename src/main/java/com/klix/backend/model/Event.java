package com.klix.backend.model;

import java.time.LocalDate;
import java.time.LocalTime;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;


/**
 * 
 */
@Getter
@Setter
@Entity
public class Event
{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message="{root.validator.notEmpty}")
    private String name;

    @NotBlank(message="{root.validator.notEmpty}")
    private String description;
    
    private LocalDate date;

    private LocalTime start;
    
    private LocalTime end;

    public Event() {}

    /**
     * constructor used in GroupAddEventViewModel
     */
    public Event(Event event) {
        this.id = event.id;
        this.name = event.name;
        this.description = event.description;
        this.date = event.date;
        this.start = event.start;
        this.end = event.end;
    }
}