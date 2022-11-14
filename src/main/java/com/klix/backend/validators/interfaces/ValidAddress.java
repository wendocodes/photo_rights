package com.klix.backend.validators.interfaces;
import java.lang.annotation.ElementType;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.annotation.RetentionPolicy;
import javax.validation.Constraint;


import javax.validation.Payload;
import com.klix.backend.validators.AddressValidator;


@Constraint(validatedBy = {AddressValidator.class})
@Target( {
    ElementType.METHOD,
    ElementType.FIELD
})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidAddress {

    String message() default "{root.validator.addressValidator.Address.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};      
}
