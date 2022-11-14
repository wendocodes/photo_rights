package com.klix.backend.model;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Transient;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.klix.backend.enums.RegistrationStatus;
import com.klix.backend.validators.interfaces.EmailConstraint;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.Period;
import java.util.HashSet;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;

/**
 * Model für die Personen
 */
@Getter
@Setter
@Entity
public class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Size(min = 1, max = 64, message = "{root.validator.userValidator.firstNameConstraints.length}")
    @Pattern(regexp = "[\\p{L}\\p{M}\\p{Zs}\\-]+", message = "{root.validator.userValidator.firstNameConstraints.pattern}")
    private String firstName;

    @Size(min = 1, max = 64, message = "{root.validator.userValidator.lastNameConstraints.length}")
    @Pattern(regexp = "[\\p{L}\\p{M}\\p{Zs}\\-]+", message = "{root.validator.userValidator.lastNameConstraints.pattern}")
    private String lastName;
    
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthdate;

    @Enumerated(value = EnumType.STRING)
    private RegistrationStatus status = RegistrationStatus.ADDED;
 
    @Transient//does not persist variable in the database
    private Integer age;
    
    private Integer pin;

    @EmailConstraint
    private String email;
 
    // Die Addresse einer Person
    @OneToMany(fetch = FetchType.EAGER, cascade=CascadeType.ALL)
    private Set<Address> address;

    // Die zugeordneten Personen - Mündel, Eltern/Legal Guardians-Person model
    @ManyToMany(fetch = FetchType.EAGER, cascade =CascadeType.MERGE)
    private Set<Person> legalGuardians = new HashSet<>();


    public Person() {}

    /**
     * Konstruktor für ViewModels
     */
    public Person(Person person)
    {
        this.address = person.address;
        this.legalGuardians = person.legalGuardians;
        this.email = person.email;
        this.birthdate = person.birthdate;
        this.id = person.id;
        this.lastName = person.lastName;
        this.firstName = person.firstName;
        this.status = person.status;
        this.pin = person.pin;
    }

    //constructor for create Parents
    public Person(String firstName, String lastName, String email, Integer pin, RegistrationStatus status) 
    {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.pin = pin;
        this.status = status;
    }

    //constructor for create child
    public Person(String firstName, String lastName, LocalDate dateOfBirth, Person guardian, Long clientId,RegistrationStatus status) 
    {
        this.firstName = firstName;
        this.lastName = lastName;
        this.birthdate = dateOfBirth;
        this.status = status;
        this.legalGuardians.add(guardian);
    }

    /**
     * Utility Methode um Adressen zu entfernen.
     */
    public void addAddress(Address address)
    {
        if (this.address == null)
        {
            this.address = new HashSet<>();
        }

        this.address.add(address);
    }


    /**
     * Utility Methode um Adressen hinzuzufügen.
     */
    public void removeAddress(Address address)
    {
        this.address.remove(address);
    }


    /**
     * Utility Methode um Personen hinzuzufügen.
     */
    public void addPerson(Person person) {
        if (this.legalGuardians == null)
        {
            this.legalGuardians = new HashSet<>();
        }

        this.legalGuardians.add(person);      
    }


    /**
     * Utillity Methode um Personen zu löschen.
     */
    public void removePerson(Person person)
    {
        this.legalGuardians.remove(person);
    }

    /**
     * Utility Method that calculates and returns age of the child
     */
    public Integer getAge(){
        if(birthdate == null) {

            return null;
            
            }    
            
            return Period.between(birthdate, LocalDate.now()).getYears();
    }
    /**
     * A string representation of a user. Contains only direct firlds and no relations to avoid lazy loading issues.
     * 
     * @return representation of a user as string
     */
    @Override
    public String toString() {
        return String.format("User(id=%d, firstName=%s, lastName=%s, email=%s)", this.id, this.firstName, this.lastName, this.email);
    }
}