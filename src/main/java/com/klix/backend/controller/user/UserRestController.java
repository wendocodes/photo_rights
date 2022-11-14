package com.klix.backend.controller.user;

import java.util.HashSet;
import java.util.Locale;

import com.klix.backend.controller.BaseController;

import com.klix.backend.model.Address;
import com.klix.backend.model.Person;
import com.klix.backend.model.User;

import com.klix.backend.service.interfaces.SecurityService;
import com.klix.backend.viewmodel.PersonAddPersonViewModel;
import com.klix.backend.viewmodel.PersonAddressViewModel;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;


@Slf4j
@RestController
@RequestMapping("/user/*")
public class UserRestController extends BaseController
{
    @Autowired MessageSource messageSource;
    
    @Autowired protected SecurityService securityService;

    /**
     * Die Funktion soll die dem angemeldeten Benutzer bzw. die seiner Person
     * zugeordneten Personen zurückgeben.
     */
    @GetMapping("/persons")
    public @ResponseBody Iterable<PersonAddressViewModel> listPersons()
    {
        User user = securityService.findLoggedInUser();
        Person userPerson = user.getPerson();

        // Die zugeordneten Personen
        Iterable<Person> legalGuardians = userPerson.getLegalGuardians();

        HashSet<PersonAddressViewModel> tableData = new HashSet<>();
        String localDateFormat = messageSource.getMessage("root.localDateFormat", null, LocaleContextHolder.getLocale());
        
        // Alle Daten
        for (Person person : legalGuardians){
            Address address = person.getAddress().stream().findFirst().orElse(new Address());
            PersonAddressViewModel row = new PersonAddressViewModel(new PersonAddPersonViewModel(person, localDateFormat), address);
            tableData.add(row);
        }

        return tableData;
    }

    /**
     * Post: Diese Methode löscht assozierte Personen vom personmodel. 
     * Das ist zum Beispiel der Fall beim löschen von Kindern von Klix Benutzern.
     * 
     * @param ids Die Liste mit den zum Löschen ausgewahlten Id's
     */
    @PostMapping("/persons_delete")
    public @ResponseBody ResponseEntity<String> delete_person(@RequestBody long[] ids, Locale locale) 
    {
        // Guards
        if(!checkPersonIds(ids)) return new ResponseEntity<>("{}", HttpStatus.BAD_REQUEST);
        
        for (long id: ids) {
            personService.deleteById(id);
        }

        return new ResponseEntity<>("{}", HttpStatus.OK);
    }
}
