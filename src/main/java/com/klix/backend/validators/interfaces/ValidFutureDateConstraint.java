package com.klix.backend.validators.interfaces;


import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;

import javax.validation.Constraint;
import javax.validation.Payload;

import com.klix.backend.validators.ValidFutureDateValidator;


/**
 * Constraint für einen Future date Validator
 */
@Documented
@Constraint(validatedBy = ValidFutureDateValidator.class)
@Target( {
    ElementType.METHOD,
    ElementType.FIELD
})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidFutureDateConstraint
{
    String message() default "Date is in the past";
    String pattern_location() default "";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
