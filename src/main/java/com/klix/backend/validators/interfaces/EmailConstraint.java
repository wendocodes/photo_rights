package com.klix.backend.validators.interfaces;

import static java.lang.annotation.ElementType.*;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;

import static java.lang.annotation.RetentionPolicy.RUNTIME;


/**
 * Warning: message - parameter will not be used, change in @javax.validation.constraints.Email instead
 */
@javax.validation.constraints.Email(message="{root.validator.userValidator.emailConstraints}", regexp="(.+@.+\\..+)?")
@Target({ METHOD, FIELD, ANNOTATION_TYPE })
@Retention(RUNTIME)
@Constraint(validatedBy = {})
public @interface EmailConstraint {

    String message() default "- never used -";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
