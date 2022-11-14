package com.klix.backend.viewmodel;

import com.klix.backend.model.Person;
import com.klix.backend.repository.PersonRepository;
import com.klix.backend.repository.UserRepository;
import com.klix.backend.validators.interfaces.EmailConstraint;
import com.klix.backend.validators.interfaces.UniqueConstraint;

import javax.validation.constraints.NotBlank;

import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
@UniqueConstraint.List({
    @UniqueConstraint(
        repo = PersonRepository.class,
        field = "email",
        message = "{root.user.add_person.email_notUnique}"
    ),
    @UniqueConstraint(
        repo = UserRepository.class,
        field = "email",
        message = "{root.user.add_person.email_notUnique}"
    )
})
@Slf4j
@Getter
@Setter
@Component
public class ChildGuardianRegistrationViewModel extends Person
{
   
    private long childId;

    private Person person;
    
    @NotBlank(message="{root.validator.noEmptyFields}")   
    @EmailConstraint
    private String email;


    private int numberLegalGuardian;

    public ChildGuardianRegistrationViewModel(){}

    /**
     * Superclass-Konstruktor für den Wrapper zum erweitern einer Person
     * für einen View der den Person und Kind validiert.
     */
    public ChildGuardianRegistrationViewModel(      @NonNull Person person, 
                                                    long childId,
                                                    String email                                           
                                            ) {
        super(person);
        this.childId = childId;
        this.email = email;
    }

    
}