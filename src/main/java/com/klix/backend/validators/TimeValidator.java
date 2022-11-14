package com.klix.backend.validators;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.klix.backend.validators.interfaces.ValidTimeConstraint;

import lombok.extern.slf4j.Slf4j;


/**
 * Ein String-Time Validator der prüft ob der String ein valides Zeitformat enthält.
 * Das Zeitformat wird aktuell in "pattern" hinterlegt. Sollte dynamisch werden.
 */
@Slf4j
public class TimeValidator implements ConstraintValidator<ValidTimeConstraint, String>
{
    /**
     * Der String der das internationalisierte Date-Pattern enthält
     */
    private String pattern_location;


    /**
     * Constraint für einen String-Date Validator
     */
    @Override
    public void initialize(ValidTimeConstraint timeConstraint) {
        this.pattern_location = timeConstraint.pattern_location(); // Lokales Time-Pattern laden
    }


    /**
     * Check des String-Datumswertes mit dem DateTimeFormatter
     */
    @Override
    public boolean isValid(String timeField, ConstraintValidatorContext cxt)
    {        
        boolean valid = false;

        try
        {
            String pattern = getInternationalizedTimePattern();

            if (pattern.length() == 0 ||  !pattern.contains("HH") || !pattern.contains("mm")){
                log.warn("Internationalisiertes Zeitpattern ist wahrscheinlich fehlerhaft definiert: " + pattern);
            }

            if (timeField != null)
            {
                LocalTime.parse(timeField, DateTimeFormatter.ofPattern(pattern).withResolverStyle(ResolverStyle.STRICT));

                valid = true;
            }
        }
        catch (DateTimeParseException e)
        {
            e.printStackTrace();
            log.error("Validating: dateField: " + e.getMessage());
            valid = false;
        }

        return valid;
    }

    
    /**
     * Die Methode holt das Date-Pattern der eingestellten Internationalisierung
     */
    private String getInternationalizedTimePattern()
    {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasenames("lang/messages");
        messageSource.setDefaultEncoding("UTF-8");
        return messageSource.getMessage(pattern_location, null, LocaleContextHolder.getLocale());
    }    
}
