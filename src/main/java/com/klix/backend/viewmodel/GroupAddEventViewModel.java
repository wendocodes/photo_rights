package com.klix.backend.viewmodel;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import javax.persistence.Transient;
import javax.validation.constraints.NotBlank;

import com.klix.backend.model.Event;
import com.klix.backend.validators.interfaces.ValidDateConstraint;
import com.klix.backend.validators.interfaces.ValidFutureDateConstraint;
import com.klix.backend.validators.interfaces.ValidTimeConstraint;
import com.klix.backend.validators.interfaces.ValidStartEndTimeConstraint;


import org.springframework.stereotype.Component;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.Getter;
import lombok.Setter;



@Slf4j
@Getter
@Setter
@Component
@ValidStartEndTimeConstraint(message="{root.validator.eventValidator.startEndTimeConstraints}")
public class GroupAddEventViewModel extends Event {

    @Transient
    @NotBlank(message="{root.validator.notEmpty}")
    @ValidDateConstraint(pattern_location="root.localDateFormat", message="{root.validator.userValidator.dateConstraints}")
    @ValidFutureDateConstraint(message="{root.validator.eventValidator.dateFutureConstraint}")
    private String dateString;

    @Transient
    @NotBlank(message="{root.validator.notEmpty}")
    @ValidTimeConstraint(pattern_location="root.localTimeFormat", message="{root.validator.eventValidator.timeConstraints}")
    private String startString;

    @Transient
    @NotBlank(message="{root.validator.notEmpty}")
    @ValidTimeConstraint(pattern_location="root.localTimeFormat", message="{root.validator.eventValidator.timeConstraints}")
    private String endString;


    // wird vom StartEndTimeValidator gesetzt und genutzt im Fehlermeldung im View anzuzeigen.
    private boolean startEndTime = false;

    public GroupAddEventViewModel() {}

    public GroupAddEventViewModel(@NonNull Event event, String localDateFormat, String localeTimeFormat) 
    {
        super(event);
        loadDateStringFromModel(localDateFormat, localeTimeFormat);
        
    }

    public void saveDateTimeStringToModel(String localDateFormat, String localeTimeFormat){
        if (dateString != null && startString != null && endString != null)
        {
        
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(localDateFormat);
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern(localeTimeFormat);

            setDate(LocalDate.parse(dateString, dateFormatter));
            setStart(LocalTime.parse(startString, timeFormatter));
            setEnd(LocalTime.parse(endString, timeFormatter));
            
        }
    }

    public void loadDateStringFromModel(String localDateFormat, String localeTimeFormat){
            
        if (getDate() != null && getStart() != null && getEnd() != null)
        {
            dateString = getDate().format(DateTimeFormatter.ofPattern(localDateFormat));
            startString = getStart().format(DateTimeFormatter.ofPattern(localeTimeFormat));
            endString = getEnd().format(DateTimeFormatter.ofPattern(localeTimeFormat));
        } else {
            log.info("DateString or StartString or EndString is null or not valid at GroupAddEventViewModel.");
        }
    }
}
