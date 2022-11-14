package com.klix.backend.validators.interfaces;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.annotation.RetentionPolicy;
import javax.validation.Constraint;
import javax.validation.Payload;

import com.klix.backend.validators.FieldsValueMatchValidator;

/**
 * Checks if two fields of a model class have the same value.
 * Sample usage on a model:
 * 
 * 
 * @FieldsValueMatch.List({ 
 *       FieldsValueMatch(
 *           field = "fieldname", 
 *           fieldMatch = "otherFieldname", 
 *           message = "some message on mismatch"
 *       ),
 *   @FieldsValueMatch(
 *           field = "w", 
 *           fieldMatch = "w1", 
 *           message = "error ..."
 *       ),
 *   })
 *   class User {
 *       private Type fieldname; private OtherType otherFieldname; ...
 *   }
 */
@Constraint(validatedBy = FieldsValueMatchValidator.class)
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface FieldsValueMatch {
 
    String message() default "Fields values don't match!";

    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
 
    String field();
 
    String fieldMatch();

    boolean containsPassword() default false;
 

    /**
     * 
     */
    @Target({ ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @interface List {
        FieldsValueMatch[] value();
    }
}