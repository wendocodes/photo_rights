package com.klix.backend.model;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OneToOne;
import javax.persistence.Transient;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import com.klix.backend.repository.UserRepository;
import com.klix.backend.validators.groups.ClearPassword;
import com.klix.backend.validators.interfaces.EmailConstraint;
import com.klix.backend.validators.interfaces.FieldsValueMatch;
import com.klix.backend.validators.interfaces.UniqueConstraint;
import com.klix.backend.validators.interfaces.ValidPassword;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


/**
 * User Model
 */
@FieldsValueMatch.List({ 
    @FieldsValueMatch(
      field = "password",
      fieldMatch = "passwordConfirm",
      containsPassword = true,
      message = "{root.validator.userValidator.passwordsNotIdenical}"
    )
})
@UniqueConstraint.List({
    @UniqueConstraint(
        repo = UserRepository.class,
        field = "username",
        message = "{root.validator.userValidator.nameNotAvailable}"
    ),
    @UniqueConstraint(
        repo = UserRepository.class,
        field = "email",
        message = "{root.validator.userValidator.nameNotAvailable}"
    )
})
@Getter
@Setter
@NoArgsConstructor
@Entity
public class User
{
    /**
     * 
     */
   
    public User(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
    }

	@Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message="{root.validator.notEmpty}")
    @Size(min=6, max=32, message="{root.validator.userValidator.usernameConstraints}")
    private String username;

    @NotBlank(message="{root.validator.notEmpty}")
    @ValidPassword(groups = ClearPassword.class)
    private String password;

    @NotBlank(message="{root.validator.notEmpty}")
    @EmailConstraint
    private String email;
    
    @Valid
    @OneToOne
    private Person person;
   
    @Transient
    private String passwordConfirm;

    @Column(name = "reset_password_token")
    private String resetPasswordToken;

    @ManyToMany(fetch = FetchType.EAGER)
    private Set<Role> roles;

    /**
     * Utillity Methode um Rollen hinzuzufügen.
     */
    public void addRole(Role role) 
    {
        if (this.roles == null) 
        {
            this.roles = new HashSet<>();
        }

        this.roles.add(role);
    }
  
    /**
     * Utillity Methode um Rollen zu entfernen.
     */
    public void removeRole(Role role) {
        this.roles.remove(role);
    }
    
    /**
     * A string representation of a user. Contains only direct firlds and no relations to avoid lazy loading issues.
     * 
     * @return representation of a user as string
     */
    @Override
    public String toString() {
        return String.format("User(id=%d, username=%s, email=%s, password=%s, passwordConfirm=%s)", this.id, this.username, this.email, this.password, this.passwordConfirm);
    }

}