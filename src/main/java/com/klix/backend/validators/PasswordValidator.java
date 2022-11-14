package com.klix.backend.validators;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.klix.backend.validators.interfaces.ValidPassword;

import org.passay.*;
import org.passay.spring.SpringMessageResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


/**
 * 
 */
public class PasswordValidator implements ConstraintValidator<ValidPassword, String>
{
    @Autowired
    private MessageSource messageSource;
    

    /**
     * 
     */
    @Override
    public boolean isValid(String password, ConstraintValidatorContext context)
    {
        MessageResolver messageResolver = new SpringMessageResolver(messageSource);
        org.passay.PasswordValidator validator =
            new org.passay.PasswordValidator(messageResolver, Arrays.asList(new LengthRule(6, 12),
                                                                            new CharacterRule(GermanCharacterData.UpperCase, 1),    // at least one upper-case character
                                                                            new CharacterRule(GermanCharacterData.LowerCase, 1),    // at least one lower-case character
                                                                            new CharacterRule(EnglishCharacterData.Digit, 1),       // at least one digit character
                                                                            new CharacterRule(EnglishCharacterData.Special, 1) ));  // at least one symbol (special character)

        RuleResult result = validator.validate(new PasswordData(password));

        if (result.isValid()) return true;

        List<String> messages = validator.getMessages(result);
        String messageTemplate = messages.stream().collect(Collectors.joining(" "));
        context.buildConstraintViolationWithTemplate(messageTemplate).addConstraintViolation().disableDefaultConstraintViolation();
                
        return false;
    }
}