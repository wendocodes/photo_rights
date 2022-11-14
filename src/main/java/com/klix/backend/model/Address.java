package com.klix.backend.model;
import javax.persistence.*;

import com.klix.backend.validators.interfaces.ValidAddress;

import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

/**
 * Model für die Adressen
 */
@Getter
@Setter
@Entity
@Component
public class Address
{
    public Address () {}

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @ValidAddress(message = "{root.validator.addressValidator.sizeStreet}")
    private String street;
    @ValidAddress(message = "{root.validator.addressValidator.sizeStreet}")
    private String number;
    @ValidAddress(message = "{root.validator.addressValidator.sizeNumber}")
    private String city;
    @ValidAddress(message = "{root.validator.addressValidator.sizePostalCode} {root.validator.addressValidator.postal_codeDigit}")
    private String postal_code;
}