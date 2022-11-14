package com.klix.backend.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import com.klix.backend.repository.ClientRepository;
import com.klix.backend.validators.interfaces.UniqueConstraint;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;


/**
 * 
 */
@Getter
@Setter
@NoArgsConstructor
@UniqueConstraint.List({
    @UniqueConstraint(
        repo = ClientRepository.class,
        field = "name",
        message = "{root.validator.userValidator.nameNotAvailable}"
    )
})
@Entity
public class Client
{
    /**
     * 
     */
    public Client(String name) {
        this.name = name;
    }

	@Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message="{root.validator.notEmpty}")
    @Size(min=6, max=32, message="{root.validator.userValidator.nameConstraints}")
    private String name;
}