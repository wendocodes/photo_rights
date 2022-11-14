package com.klix.backend.validators;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.klix.backend.validators.interfaces.ValidStartEndTimeConstraint;
import com.klix.backend.viewmodel.GroupAddEventViewModel;


/**
 * Validator überprüft, ob ein gegebener Startzeitpunkt vor dem gegebenen Endzeitpunkt liegt. 
 * Es wird ein Boolean startEndTime im GroupAddEventViewModel gesetzt, er genutzt wird um im 
 * event_add oder event_edit formular eine Fehlermeldung anzuzeigen.
 */
public class StartEndTimeValidator implements ConstraintValidator<ValidStartEndTimeConstraint, GroupAddEventViewModel> {

    @Override
    public void initialize(ValidStartEndTimeConstraint constraintAnnotation) {
    }

    @Override
    public boolean isValid(GroupAddEventViewModel event, ConstraintValidatorContext context) 
    {
        String[] start_tokens = event.getStartString().split(":");
        String[] end_tokens = event.getEndString().split(":");

        boolean valid = false;

        if (start_tokens.length == 2 &&  start_tokens.length ==end_tokens.length) 
        {

            if (Integer.parseInt(start_tokens[0]) < Integer.parseInt(end_tokens[0]) || 
            Integer.parseInt(start_tokens[0]) == Integer.parseInt(end_tokens[0]) && Integer.parseInt(start_tokens[1]) < Integer.parseInt(end_tokens[1])) 
            {
                event.setStartEndTime(false);
                valid = true;
            } 
            else 
            {
                event.setStartEndTime(true);
            }
        }
        return valid;
    }
    
}
