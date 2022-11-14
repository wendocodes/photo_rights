package com.klix.backend.validators;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.klix.backend.validators.interfaces.ValidDateConstraint;

import lombok.extern.slf4j.Slf4j;


/**
 * Ein String-Date Validator der prüft ob der String ein valides Datum enthält.
 * Das Datumsformat wird aktuell in "pattern" hinterlegt. Sollte dynamisch werden.
 */
@Slf4j
public class DateValidator implements ConstraintValidator<ValidDateConstraint, String>
{
    /**
     * Der String der das internationalisierte Date-Pattern enthält
     */
    private String pattern_location;


    /**
     * Constraint für einen String-Date Validator
     */
    @Override
    public void initialize(ValidDateConstraint dateConstraint) {
        this.pattern_location = dateConstraint.pattern_location(); // Lokales Date-Pattern laden
    }


    /**
     * Check des String-Datumswertes mit dem DateTimeFormatter
     */
    @Override
    public boolean isValid(String dateField, ConstraintValidatorContext cxt)
    {        
        boolean valid = false;
        
        try
        {
            String pattern = getInternationalizedDatePattern();

            if (pattern.length() == 0 || !pattern.contains("uuuu") || !pattern.contains("MM") || !pattern.contains("dd")){
                log.warn("Internationalisiertes Datumspattern ist wahrscheinlich fehlerhaft definiert: " + pattern);
            }
            log.info(pattern);
            /**
             * ResolverStyle.STRICT for 30, 31 days checking, and also leap year. yyyy muss hier mit uuuu ersetzt werden: 
             * https://stackoverflow.com/questions/60779696/localdate-datetimeformatter-problems
             */
            if (dateField != null)
            {
                LocalDate.parse(dateField,
                        DateTimeFormatter.ofPattern(pattern)
                                .withResolverStyle(ResolverStyle.STRICT)
                );

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
    private String getInternationalizedDatePattern()
    {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasenames("lang/messages");
        messageSource.setDefaultEncoding("UTF-8");
        return messageSource.getMessage(pattern_location, null, LocaleContextHolder.getLocale());
    }
}