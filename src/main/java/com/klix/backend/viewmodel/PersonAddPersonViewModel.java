package com.klix.backend.viewmodel;

import com.klix.backend.model.Person;

import com.klix.backend.validators.interfaces.ValidDateConstraint;
import com.klix.backend.validators.interfaces.DateIntervalConstraint;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import javax.persistence.Transient;
import javax.validation.constraints.NotBlank;

import lombok.extern.slf4j.Slf4j;
import lombok.Getter;
import lombok.Setter;

/**
 * WrapperModel / ViewModel für die Personen, um einen nur im person_add und person_edit View validiertes
 * "PersonBirthdateString"-Feld bereitzustellen und die Konversion von String auf LocalDate durchzuführen.
 */
@Slf4j
@Getter
@Setter
@Component
public class PersonAddPersonViewModel extends Person
{
    /**
     * Das Feld wirt in user/person_add und user/person_edit verwendet. Die dort eingegebenen
     * Personen (Mündel) müssen ein Geburtsdatum bekommen. Sonst ist das Geburtsdatum nicht nötig.
     */
    @Transient
    @NotBlank(message="{root.validator.notEmpty}")
    @ValidDateConstraint(pattern_location="root.localDateFormat", message="{root.validator.userValidator.dateConstraints}")
    @DateIntervalConstraint(pattern_location="root.localDateFormat", olderThan="0,0,0",  message="{root.validator.userValidator.alreadyBorn}")
    private String birthdateString;


    public PersonAddPersonViewModel() {}

    /**
     * Superclass-Konstruktor für den Wrapper zum erweitern einer Person
     * für einen View der den "birthdateString" validiert.
     */
    public PersonAddPersonViewModel(@NonNull Person person, String localDateFormat) {
        super(person);
        loadBirthdateStringFromModel(localDateFormat);
    }


    /**
     * Konvertiert den Datumsstring des Geburtstages in das Person-Model LocalDate Format
     */
    public void saveBirthdateStringToModel(String localDateFormat){
        if (birthdateString != null){
            log.debug("birthdateString " + birthdateString);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(localDateFormat);
            setBirthdate(LocalDate.parse(birthdateString, formatter));
        }
    }

    /**
     * Konvertiert das LocalDate Feld "Geburtsdatum" in einen Datumsstring nach dem eingestellten
     * lokalen Datumsformat
     */
    public void loadBirthdateStringFromModel(String localDateFormat){
        if (getBirthdate() != null){
            birthdateString = getBirthdate().format(DateTimeFormatter.ofPattern(localDateFormat));
        }
    }

    public Person getPerson() {
        Person person =  new Person();
        person.setFirstName(this.getFirstName());
        person.setLastName(this.getLastName());
        person.setBirthdate(this.getBirthdate());
        person.setEmail(this.getEmail());

        return person;
    }
 
}