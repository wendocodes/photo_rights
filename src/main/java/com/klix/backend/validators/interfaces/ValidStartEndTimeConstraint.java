package com.klix.backend.validators.interfaces;


import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;

import javax.validation.Constraint;
import javax.validation.Payload;

import com.klix.backend.validators.StartEndTimeValidator;

@Documented
@Constraint(validatedBy = StartEndTimeValidator.class)
@Target( {
    ElementType.TYPE
})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidStartEndTimeConstraint
{
    String message() default "Invalid date";
    String pattern_location() default "";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
