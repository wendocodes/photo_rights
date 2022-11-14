package com.klix.backend.validators;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.klix.backend.validators.interfaces.FieldsValueMatch;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;


/**
 * 
 */
public class FieldsValueMatchValidator implements ConstraintValidator<FieldsValueMatch, Object>
{
  private String field;
  private String fieldMatch;
  private boolean containsPassword;

  @Autowired
  private BCryptPasswordEncoder passwordEncoder;


  /**
   * 
   */
  @Override
  public void initialize(FieldsValueMatch constraintAnnotation)
  {
      this.field = constraintAnnotation.field();
      this.fieldMatch = constraintAnnotation.fieldMatch();

      this.containsPassword = constraintAnnotation.containsPassword();
  }


  /**
   * 
   */
  @Override
  public boolean isValid(Object model, ConstraintValidatorContext context)
  {
    BeanWrapper modelWrapper = new BeanWrapperImpl(model);
    String fieldValue = (String)modelWrapper.getPropertyValue(field);
    String fieldMatchValue = (String)modelWrapper.getPropertyValue(fieldMatch);
    
    if (fieldValue != null)
    {
      // case without password included
      boolean simpleEquality = fieldValue.equals(fieldMatchValue);
      if (!this.containsPassword || simpleEquality) return simpleEquality;

      // case with encoded password
      if (passwordEncoder == null) return true;   // only during initialization, ignore

      // check encrypted password
      return passwordEncoder.matches(fieldMatchValue, fieldValue);
    }
    return fieldMatchValue == null;
  }
}