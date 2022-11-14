package com.klix.backend.validators.interfaces;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import com.klix.backend.validators.UniqueValidator;

import org.springframework.data.repository.CrudRepository;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Checks each Unique constraint of @Unique.List seperately for duplicates.
 * A duplicate exists if the value of the specified field as well as the id match.
 * 
 * The search is not case sensitive on Strings!
 * 
 * Needs:
 *   - a getter for the field and id on the model ( format: get[field | 'Id']() )
 *   - a lookup for the field on specified repo ( format: Collection<Model> findBy[Field](FieldType field) )
 * Note: functions have to be camelCase.
 * 
 * See User model as example.
 */
@Target(ElementType.TYPE)
@Retention(RUNTIME)
@Constraint(validatedBy={UniqueValidator.class})
public @interface UniqueConstraint {

  String message() default "Field is unique but identified a duplicate";

  Class<?>[] groups() default { };

  Class<? extends Payload>[] payload() default { };

  Class<? extends CrudRepository<?, Long>> repo();
  
  String field() default "name";


  /**
   * 
   */
  @Target({ ElementType.TYPE })
    @Retention(RUNTIME)
    @interface List {
        UniqueConstraint[] value();
    }
}