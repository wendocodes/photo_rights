package com.klix.backend.viewmodel;

import javax.persistence.Transient;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import com.klix.backend.model.Person;
import com.klix.backend.repository.UserRepository;
import com.klix.backend.validators.groups.ClearPassword;
import com.klix.backend.validators.interfaces.DateIntervalConstraint;
import com.klix.backend.validators.interfaces.FieldsValueMatch;
import com.klix.backend.validators.interfaces.UniqueConstraint;
import com.klix.backend.validators.interfaces.ValidDateConstraint;
import com.klix.backend.validators.interfaces.ValidPassword;

import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@FieldsValueMatch.List({ 
    @FieldsValueMatch(
      field = "password",
      fieldMatch = "passwordConfirm",
      containsPassword = true,
      message = "{root.validator.userValidator.passwordsNotIdentical}"
    )
})
@UniqueConstraint.List({
    @UniqueConstraint(
        repo = UserRepository.class,
        field = "username",
        message = "{root.validator.userValidator.nameNotAvailable}"
    )
})
@Getter
@Setter
@Component
public class GuardianRegistrationViewModel extends Person {

    @NotBlank(message="{root.validator.notEmpty}")
    @Size(min=6, max=32, message="{root.validator.userValidator.usernameConstraints}")
    private String username;

    @NotBlank(message="{root.validator.notEmpty}")
    @ValidPassword(groups = ClearPassword.class)
    private String password;

    @Transient
    private String passwordConfirm;

    private String token;

    @Transient
    @NotBlank(message="{root.validator.notEmpty}")
    @ValidDateConstraint(pattern_location="root.localDateFormat", message="{root.validator.userValidator.dateConstraints}")
    @DateIntervalConstraint(pattern_location="root.localDateFormat", olderThan="0,0,0",  message="{root.validator.userValidator.alreadyBorn}")
    private String birthdateString;
   
    public Person person;

    public GuardianRegistrationViewModel(){}

    public GuardianRegistrationViewModel(Person person) {
        super(person);
    }
}
