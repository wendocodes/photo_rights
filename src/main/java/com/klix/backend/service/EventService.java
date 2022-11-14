package com.klix.backend.service;

import java.util.Set;

import com.klix.backend.model.Event;
import com.klix.backend.model.Groups;
import com.klix.backend.repository.EventRepository;
import com.klix.backend.repository.GroupsRepository;
import com.klix.backend.viewmodel.GroupAddEventViewModel;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.context.MessageSource;

import lombok.extern.slf4j.Slf4j;


@Service
@Slf4j 
public class EventService {

    @Autowired private GroupsService groupsService;

    @Autowired private GroupsRepository groupsRepository;

    @Autowired private EventRepository eventRepository;

    @Autowired private MessageSource messageSource;


    /**
     * delete event of this group
     */
    public void deleteEventById(Long groups_id, Long event_id) {

        Long[] groupId = eventRepository.findGroups(event_id);

        for (Long id : groupId) {
            try {
                eventRepository.deleteAssociatedEvent(id, event_id);
                eventRepository.deleteById(event_id);
            } catch (IllegalArgumentException e) {
                log.info("eventId or groupId is null at deleteEventById in EventService.");
            }
            
        }
    }

    /**
     * delete event-group association
     */
    public void deleteAssociatedEvent(Long groups_id, Long event_id) {
        if(groups_id != null && event_id != null) {
        eventRepository.deleteAssociatedEvent(groups_id, event_id);
        } else {
            log.info("EventId or groupId is null at deleteAssociatedEvent in eventService.");
        }

    }


    public void saveEvent(Event event) {
        if(event != null) {
        eventRepository.save(event);
        } else {
            log.info("Event is null at saveEvent in eventService.");
        }
    }

    
    public void addEvent(Event event, Long groupsId) {

        Groups group = groupsService.findById(groupsId);
        Set<Event> events = group.getEvents();
    
        if (event != null)  {
            events.add(event);
            group.setEvents(events);
            groupsRepository.save(group);
        } else {
            log.info("Event is null at addEvent in eventService.");
        }

    }

    public void editEvent(Long eventId, GroupAddEventViewModel eventViewModel) {

        // Nach der Validation sollte der "BirthdateString" String ein valides Datum enthalten
        String localDateFormat = messageSource.getMessage("root.localDateFormat", null, LocaleContextHolder.getLocale());
        String localeTimeFormat = messageSource.getMessage("root.localTimeFormat", null, LocaleContextHolder.getLocale());
        eventViewModel.saveDateTimeStringToModel(localDateFormat, localeTimeFormat);
        Event event = new Event(eventViewModel);

        Event existingEvent = eventRepository.findById(eventId).orElse(null);

        if( existingEvent == null) {
            log.info("Event not found on editEvent in EventService.");
        } else {

        existingEvent.setDate(event.getDate());
        existingEvent.setName(event.getName());
        existingEvent.setDescription(event.getDescription());
        existingEvent.setStart(event.getStart());
        existingEvent.setEnd(event.getEnd());

        eventRepository.save(existingEvent);
        
        }
    }
    
}
