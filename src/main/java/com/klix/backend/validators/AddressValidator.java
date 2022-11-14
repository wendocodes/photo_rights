package com.klix.backend.validators;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import com.klix.backend.validators.interfaces.ValidAddress;

// import java.util.regex.*;


public class AddressValidator implements ConstraintValidator<ValidAddress,  String> {
    
    private int min=2;
    private int max=100;

    
    /**
     * Override the isValid method in the ConstraingValidator class
     * @returns valid Adress after the check
     */
    @Override
    public boolean isValid(String streetField, ConstraintValidatorContext cxt)
    {        
        
        // Regex to check string contains only digits
        String regex = "[0-9]+";
    
        // Compile the ReGex
        //Pattern p = Pattern.compile(regex);

        boolean valid = true;

        // if the addressfield is not empty check that the length is greater than 0, check if it contains strictly digits
        if (streetField != null && streetField.length() > 0 && streetField.matches(regex))
        {
            // if postal address, the minimum must be 5 digits and max 20
            if (streetField.length() < 5 || streetField.length() > 20){
            valid = false;
            }   
        }    // if the addressfield is expecting a string, just check the min and max values
        else if (streetField != null && streetField.length() > 0 && (streetField.length() < min || streetField.length() > max))
            {
                
                valid = false;
            }
       
        return valid;
    }
}