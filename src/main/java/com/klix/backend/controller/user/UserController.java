package com.klix.backend.controller.user;

import com.klix.backend.model.Person;
import com.klix.backend.model.PublicationResponse;
import com.klix.backend.model.User;
import com.klix.backend.controller.BaseController;
import com.klix.backend.enums.PublicationResponseStatus;
import com.klix.backend.model.Address;
import com.klix.backend.model.IdPicture;
import com.klix.backend.viewmodel.PersonAddPersonViewModel;
import com.klix.backend.viewmodel.PersonSearchViewModel;
import com.klix.backend.repository.AddressRepository;
import com.klix.backend.repository.projections.PersonPermission;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.persistence.NonUniqueResultException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/user")
public class UserController extends BaseController {

    @Autowired
    private AddressRepository addressRepository;

    /**
     * Die default-Benutzerseite mit den Links und den Einstellungen für das Profil,
     * die zugeordneten Personen (Mündel) usw.
     */
    @GetMapping
    public String userIndex(Model model, Locale locale) {

        User user = securityService.findLoggedInUser();
        Person person = user.getPerson();

        if (person != null) {
            IdPicture profileImg = idPictureRepository.findByPersonId(person.getId()).orElse(null);
            if (profileImg != null) {
                model.addAttribute("image", profileImg);
            }
            this.addClientInfo(model, person.getId());
            List<PersonPermission> permissions = getPermissions(person.getId());
            PersonPermission permission = permissions.stream()
                    .filter(p -> p.getClient_role_id() == 2 || p.getClient_role_id() == 1)
                    .findFirst().orElse(null);

            Set<PublicationResponse> pending = this.publicationResponseRepository
                    .findByStatusAndPersonId(PublicationResponseStatus.PENDING, person.getId());
            if (pending.size() > 0) {
                String pubRequestMessage = messageSource.getMessage("root.user.publication.request.emailSent", null,
                        locale);
                String emailSub = messageSource.getMessage("root.user.publication.request.emailSubject", null,
                        locale);
                emailService.sendPubRequestEmail(user.getEmail(), pubRequestMessage, emailSub);
            }
            model.addAttribute("user", user);
            model.addAttribute("pubRequest", pending.size());
            model.addAttribute("permission", permission);
            model.addAttribute("id", person.getId());
            model.addAttribute("person", person);
            model.addAttribute("pname", person.getFirstName() + " " + person.getLastName());
            model.addAttribute("id", person.getId());

            if (permission == null)
                return "user/index";
            else {
                model.addAttribute("id", permission.getClient_id());
                model.addAttribute("roleid", permission.getClient_role_id());
                model.addAttribute("permission", permission);
                return "redirect:/client/" + permission.getClient_id() + "/dashboard" + "/role/"
                        + permission.getClient_role_id();
            }
        }
        model.addAttribute("user", user);
        return "user/index";
    }

    /**
     * Get all the children added by the Kindergarten employee
     */
    @GetMapping("/children")
    public String added_children(Model model, Locale locale) {

        User user = securityService.findLoggedInUser();
        Person person = user.getPerson();

        if (person != null) {
            /* Start Andreas 12.03.2022 */
            List<Person> children = personRepository
                    .findAllById(personRepository.findChildrensIDsOfPerson(person.getId()));
            HashMap<Person, IdPicture> map = new HashMap<Person, IdPicture>();
            for (Person child : children) {
                IdPicture idPicture = idPictureRepository.findByPersonId(child.getId()).orElse(null);
                if (idPicture != null)
                    map.put(child, idPicture);
                else
                    map.put(child, PersonSearchViewModel.noPictureAvailable);
            }
            model.addAttribute("childPictureMap", map);
            /* Ende Andreas 12.03.2022 */

            this.addClientInfo(model, person.getId());
            List<PersonPermission> permissions = getPermissions(person.getId());

            PersonPermission permission = permissions.stream()
                    .filter(p -> p.getClient_role_id() == 2 || p.getClient_role_id() == 1)
                    .findFirst().orElse(null);

            model.addAttribute("permission", permission);
            model.addAttribute("id", person.getId());
            model.addAttribute("person", person);
            model.addAttribute("pname", person.getFirstName() + " " + person.getLastName());
            model.addAttribute("id", person.getId());

        }
        List<PersonPermission> permissions = getPermissions(person.getId());
        model.addAttribute("clientsInfo", permissions);
        model.addAttribute("id", person.getId());
        model.addAttribute("pname", person.getFirstName() + " " + person.getLastName());
        return "user/children";
    }

    /**
     * 
     * Edit profile
     */
    @GetMapping("edit_profile")
    public String my_profile(Model model,
            Locale locale) {
        User user = securityService.findLoggedInUser();
        Person person = user.getPerson();
        userRepository.save(user);
        this.addClientInfo(model, person.getId());
        List<PersonPermission> permissions = getPermissions(person.getId());
        PersonPermission permission = permissions.stream()
                .filter(p -> p.getClient_role_id() == 2 || p.getClient_role_id() == 1)
                .findFirst().orElse(null);
        model.addAttribute("permission", permission);
        model.addAttribute("id", person.getId());
        model.addAttribute("pname", person.getFirstName() + " " + person.getLastName());
        model.addAttribute("user", new User());
        model.addAttribute("person", person);
        return "user/edit_profile";
    }

    /**
     * 
     * !TODO: Edit profile
     */
    @PostMapping("edit_profile")
    public String save_edited(Model model,
            Locale locale) {
        User user = securityService.findLoggedInUser();
        Person person = user.getPerson();

        userRepository.save(user);
        model.addAttribute("user", user);
        model.addAttribute("person", person);
        return "user/edit_profile";
    }

    /**
     * Save für "save_editedPerson" und "add_person".
     */
    public void save_person(PersonAddPersonViewModel personViewModel, Address address) {
        Locale locale = LocaleContextHolder.getLocale();
        // Nach der Validation sollte der "BirthdateString" String ein valides Datum
        // enthalten
        String localDateFormat = messageSource.getMessage("root.localDateFormat", null, locale);
        personViewModel.saveBirthdateStringToModel(localDateFormat);
        Person person = new Person(personViewModel); // Das ist notwendig weil Hibernate auf den Referenzen arbeitet und
                                                     // nach einem Cast immer noch
                                                     // die Subklasse erkennt, das ist PersonAddPersonViewModel und dann
                                                     // einen Fehler produziert.
        // Der aktuelle User/Person
        try {
            if (address.getId() != null) {

                log.info("i'm in address  update flag");
                addressRepository.save(address);
            }
            person.addAddress(address);
            personService.save(person);
            User user = findLoggedUser(locale);
            Person userPerson = user.getPerson();
            userPerson.addPerson(person);
            personService.save(userPerson);

        } catch (NoSuchElementException e) {
            log.debug("NoSuchElementException after save_person in UserController: " + e.getMessage());
        }
    }

    /**
     * Edit (Update)
     */
    @GetMapping("/person_edit/{id}")
    public String showForm_editPerson(Model model, @PathVariable("id") long id) {
        if (checkPersonId(id)) {
            // add client data to model
            Person person = personService.findById(id);

            String localDateFormat = messageSource.getMessage("root.localDateFormat", null,
                    LocaleContextHolder.getLocale());

            PersonAddPersonViewModel personViewModel = new PersonAddPersonViewModel(person, localDateFormat);
            Address address = personViewModel.getAddress().stream().findFirst().orElse(new Address());
            log.debug("personViewModel.getId()" + personViewModel.getId());

            model.addAttribute("person", personViewModel);
            model.addAttribute("address", address);

            return "user/person_edit";
        } else {
            log.warn("person with id " + id + " does not exist");
            return "person_table"; // Sicherheitshalber hier (vorerst) keine neue Person anlegen
        }
    }

}