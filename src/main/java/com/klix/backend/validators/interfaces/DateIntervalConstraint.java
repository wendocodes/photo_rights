package com.klix.backend.validators.interfaces;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;

import javax.validation.Constraint;
import javax.validation.Payload;

import com.klix.backend.validators.DateIntervalValidator;


/**
 * Constraint für einen String-Date-Interval Validator zum Prüfen eines Alters o.ä.
 * 
 * Die Intervalle "youngerThan" und "olderThan" müssen in "Years, Months, Days"
 * angegeben werden, alles als Integer. Z. B. "18,0,0" für die Dauer von 18 Jahren.
 * Die Dauern werden mit dem aktuellen Datum verglichen.
 * 
 */
@Documented
@Constraint(validatedBy = DateIntervalValidator.class)
@Target( {
    ElementType.METHOD,
    ElementType.FIELD
})
@Retention(RetentionPolicy.RUNTIME)
public @interface DateIntervalConstraint
{
    String message() default "The date is not included in required interval";
    String pattern_location() default "";
    String youngerThan() default "";
    String olderThan() default "";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}