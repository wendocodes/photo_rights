package com.klix.backend.viewmodel;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.springframework.stereotype.Component;

import com.klix.backend.repository.PersonRepository;
import com.klix.backend.repository.UserRepository;
import com.klix.backend.validators.interfaces.EmailConstraint;
import com.klix.backend.validators.interfaces.UniqueConstraint;
import lombok.extern.slf4j.Slf4j;

import lombok.Getter;
import lombok.Setter;

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
public class AdditionalGuardianViewModel{

    Long id;

    @NotBlank(message="{root.validator.notEmpty}")
    @Size(min = 0, max = 64, message = "{root.validator.userValidator.firstNameConstraints.length}")
    @Pattern(regexp = "^$|[\\p{L}\\p{M}\\p{Zs}\\-]+", message = "{root.validator.userValidator.firstNameConstraints.pattern}")
    private String parentFirstName;

    @NotBlank(message="{root.validator.notEmpty}")
    @Size(min = 0, max = 64, message = "{root.validator.userValidator.lastNameConstraints.length}")
    @Pattern(regexp = "^$|[\\p{L}\\p{M}\\p{Zs}\\-]+", message = "{root.validator.userValidator.lastNameConstraints.pattern}")
    private String parentLastName;

    @NotBlank(message="{root.validator.noEmptyFields}")   
    @EmailConstraint
    private String email;

    public AdditionalGuardianViewModel(){}

    /**
     * Superclass-Konstruktor für den Wrapper zum erweitern einer Person
     * für einen View der den Person und Kind validiert.
     */
    public AdditionalGuardianViewModel(Long id, String parentFirstName, String parentLastName, String email) 
    {
        this.id = id;
        this.parentFirstName = parentFirstName;
        this.parentLastName = parentLastName;
        this.email = email;
    }
}
