package com.klix.backend.viewmodel;

import javax.persistence.Transient;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.klix.backend.model.Person;
import com.klix.backend.validators.interfaces.DateIntervalConstraint;
import com.klix.backend.validators.interfaces.ValidDateConstraint;

import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
public class ChildRegistrationViewModel extends Person
{
    @Size(min = 0, max = 64, message = "{root.validator.userValidator.firstNameConstraints.length}")
    @Pattern(regexp = "^$|[\\p{L}\\p{M}\\p{Zs}\\-]+", message = "{root.validator.userValidator.firstNameConstraints.pattern}")
    @NotBlank(message="{root.validator.notEmpty}")
    private String childFirstName;

    @Size(min = 0, max = 64, message = "{root.validator.userValidator.lastNameConstraints.length}")
    @Pattern(regexp = "^$|[\\p{L}\\p{M}\\p{Zs}\\-]+", message = "{root.validator.userValidator.lastNameConstraints.pattern}")
    @NotBlank(message="{root.validator.notEmpty}")
    private String childLastName;
    
    @Transient
    @NotBlank(message="{root.validator.notEmpty}")
    @ValidDateConstraint(pattern_location="root.localDateFormat", message="{root.validator.userValidator.dateConstraints}")
    @DateIntervalConstraint(pattern_location="root.localDateFormat", olderThan="0,0,0",  message="{root.validator.userValidator.alreadyBorn}")
    private String childBirthdate;

    @NotBlank(message="{root.validator.notEmpty}")
    private String numberLegalGuardian;

   


    public ChildRegistrationViewModel(){}

    /**
     * Superclass-Konstruktor für den Wrapper zum erweitern einer Person
     * für einen View der den Person und Kind validiert.
     */
    public ChildRegistrationViewModel(  String childFirstName, 
                                        String childLastName,
                                        String childBirthdate,
                                        String numberLegalGuardian
                                        ) {
        this.childFirstName = childFirstName;
        this.childLastName = childLastName;
        this.childBirthdate = childBirthdate;
        this.numberLegalGuardian = numberLegalGuardian;
    }

    
}
