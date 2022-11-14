package com.klix.backend.validators;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.springframework.context.i18n.LocaleContextHolder;

import com.klix.backend.validators.interfaces.DateIntervalConstraint;

import org.springframework.context.support.ResourceBundleMessageSource;

import lombok.extern.slf4j.Slf4j;


/**
 * Ein String-Date Validator der prüft, ob ein Datum die angegebene Relation erfüllt
 * Das Datumsformat wird aktuell in "pattern" hinterlegt. Sollte dynamisch werden.
 */
@Slf4j
public class DateIntervalValidator implements ConstraintValidator<DateIntervalConstraint, String>
{
    /**
     * Der String der das internationalisierte Date-Pattern enthält
     */
    private String pattern_location;

    private Period youngerThan;
    private Period olderThan;

    private boolean newerThanSet = false;
    private boolean olderThanSet = false;


    /**
     * Constraints für den String-Date-Interval Validator
     */
    @Override
    public void initialize(DateIntervalConstraint dateIntervalConstraint)
    {
        // Lokales Date-Pattern laden
        this.pattern_location = dateIntervalConstraint.pattern_location();

        // Parsen der Constraint Argumente - newerThan
        if (dateIntervalConstraint.youngerThan().length() > 0){
            String[] durationArray = dateIntervalConstraint.youngerThan().split("\\s*,\\s*");
            if (durationArray.length == 3){
                try {
                    int[] ymdNewerThan = {0,0,0};
                    for (int i = 0; i < 3; i++){
                        ymdNewerThan[i] = Integer.parseInt(durationArray[i]);
                    }
                    youngerThan = Period.of(ymdNewerThan[0], ymdNewerThan[1], ymdNewerThan[2]).normalized();
                    newerThanSet = true;
                }
                catch (NumberFormatException e){}
            }
        }

        // Parsen der Constraint Argumente - olderThan
        if (dateIntervalConstraint.olderThan().length() > 0){
            String[] durationArray = dateIntervalConstraint.olderThan().split("\\s*,\\s*");
            if (durationArray.length == 3){
                try {
                    int[] ymdOlderThan = {0,0,0};
                    for (int i = 0; i < 3; i++){
                        ymdOlderThan[i] = Integer.parseInt(durationArray[i]);
                    }
                    olderThan = Period.of(ymdOlderThan[0], ymdOlderThan[1], ymdOlderThan[2]).normalized();
                    olderThanSet = true;
                }
                catch (NumberFormatException e){}
            }
        }
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

            /**
             * ResolverStyle.STRICT for 30, 31 days checking, and also leap year. yyyy muss hier mit uuuu ersetzt werden: 
             * https://stackoverflow.com/questions/60779696/localdate-datetimeformatter-problems
             */
            if (dateField != null)
            {
                LocalDate date = LocalDate.parse(dateField,
                                                DateTimeFormatter.ofPattern(pattern)
                                                    .withResolverStyle(ResolverStyle.STRICT));

                LocalDate today = LocalDate.now();

                log.debug("Validations arguments are: " + dateField + " " + youngerThan + " " + olderThan);
                log.debug("Validating date: " + date.toString() + " actual date is: " + today.toString() );

                // Validieren von newerThan
                boolean newerValid = true;
                if (newerThanSet){
                    LocalDate newer = today.minusYears(youngerThan.getYears()).minusMonths(youngerThan.getMonths()).minusDays(youngerThan.getDays());

                    if (date.isBefore(newer)){
                        newerValid = false;
                    }
                }

                // Validieren von olderThan
                boolean olderValid = true;
                if (olderThanSet){
                    LocalDate older = today.minusYears(olderThan.getYears()).minusMonths(olderThan.getMonths()).minusDays(olderThan.getDays());

                    if (date.isAfter(older)){
                        olderValid = false;
                    }
                }

                valid = newerValid && olderValid;
            }
        }
        catch (DateTimeParseException e)
        {
            e.printStackTrace();
            log.error("Validating Date Interval: " + e.getMessage());
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
