package com.klix.backend.validators;

import java.time.LocalDate;
import java.time.ZoneId;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.klix.backend.validators.interfaces.ValidFutureDateConstraint;


/**
 * Dieser Validator überprüft, ob ein Datum-String in der Zukunft oder Gegenwart liegt.
 * Dieser wird im event_add und event_edit gebraucht um das Datum des Events zu überprüfen.
 */
public class ValidFutureDateValidator implements ConstraintValidator<ValidFutureDateConstraint, String> {

    @Override
    public void initialize(ValidFutureDateConstraint constraintAnnotation) {
    }

    @Override
    public boolean isValid(String datefield, ConstraintValidatorContext context) 
    {

    boolean valid = false;

    String[] date_tokens = datefield.split("\\.");
    
    ZoneId zone = ZoneId.of("UTC");
    LocalDate today = LocalDate.now(zone);

    int current_y = today.getYear();
    int current_m = today.getMonthValue();
    int current_d = today.getDayOfMonth();


    if(date_tokens.length == 3 && current_y <= Integer.parseInt(date_tokens[2]) && 
        current_m <= Integer.parseInt(date_tokens[1]) && 
        current_d <= Integer.parseInt(date_tokens[0])) 
    { 
        valid = true;
    }
    return valid;
    }
    
}