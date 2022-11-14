package com.klix.backend.controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

import javax.persistence.NonUniqueResultException;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import com.klix.backend.constants.RoleConstants;
import com.klix.backend.enums.RegistrationStatus;
import com.klix.backend.model.Client;
import com.klix.backend.model.Person;
import com.klix.backend.model.User;
import com.klix.backend.model.interfaces.Utility;
import com.klix.backend.repository.projections.PersonPermission;
import com.klix.backend.validators.groups.ClearPassword;
import com.klix.backend.viewmodel.UserPasswordResetViewModel;
import com.klix.backend.viewmodel.GuardianRegistrationViewModel;
import com.klix.backend.viewmodel.PersonRegisterViewModel;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.utility.RandomString;

/**
 * 
 */
@Slf4j
@Controller
public class HomeController extends BaseController {

    /**
     * 
     */
    @GetMapping("/")
    public String root() {
        return "index";
    }

    /**
     * 
     */
    @GetMapping("/login")
    public String login(Model model, String error, String logout) {
        return "index";
    }

    /**
     * !Sollte in den Develop Controller
     */
    @GetMapping("/develop")
    public String developerIndex() {
        return "develop/index";
    }

    /**
     * 
     */
    @GetMapping("/administration")
    public String administrationIndex(Model model) {
        return "administration/index";
    }

    /**
     * list all permissions with person & client & role set
     */
    @Transactional
    @GetMapping("/client")
    public String clientIndex(Model model, Locale locale) {
        Person person;
        try {
            User user = findLoggedUser(locale);
            person = user.getPerson();
        } catch (NoSuchElementException e) {
            model.addAttribute("error", e.getMessage());
            return "client/index";
        }

        List<PersonPermission> permissions = permissionRepository.findPermissionByPersonId(person.getId());

        List<PersonPermission> completePermissions = new ArrayList<>();
        for (PersonPermission p : permissions) {
            if (p.getId() != null && p.getClient_id() != null && p.getClient_role_id() != null)
                completePermissions.add(p);
        }

        model.addAttribute("clientsInfo", completePermissions);
        model.addAttribute("noPermissions", messageSource.getMessage("root.client.index.noPermissions", null, locale));

        return "client/index";
    }

    /**
     * Get the Legal Guardian Registration form
     */
    @GetMapping("/verify_pin")
    public String getLegalGuardianLoginForm(Model model) {
        model.addAttribute("person", new Person());

        return "verify_pin";
    }

    /**
     * Legal Guardian enters PIN for verification
     * 
     * @return a response with an ok if PIN matches
     */
    @PostMapping("/verify_pin")
    public String verifyPin(HttpServletRequest request, Model model, Locale locale) {
        String pin = request.getParameter("pin");
        if (pin == "") {
            String message = messageSource.getMessage("root.edit_person.clientLoginTitle", null, locale);
            model.addAttribute("nullPin", message);
            model.addAttribute("person", new Person());
            return "verify_pin";
        }

        Person person = personRepository.findByPin(pin);
        if (person == null) {
            String message = messageSource.getMessage("root.edit_person.clientPinWrongOrAlreadyUsed", null, locale);
            model.addAttribute("pinError", message);
            model.addAttribute("person", new Person());
            return "verify_pin";
        }

        person.setStatus(RegistrationStatus.APPROVED);
        personService.save(person);
        GuardianRegistrationViewModel viewModel = new GuardianRegistrationViewModel(person);
        model.addAttribute("userC", viewModel);

        return "create_credentials";
    }

    /**
     * 
     */
    @PostMapping("/create_credentials")
    public String updateProfile(Model model,
            RedirectAttributes redirectAttributes,
            Locale locale,
            @Validated(ClearPassword.class) @ModelAttribute("userC") @Valid GuardianRegistrationViewModel personAndUserCredentials,
            Errors errors) {
        log.info("user creds : {}", personAndUserCredentials);

        // on error: stay on page and display errors for a good user experience
        if (errors.hasErrors()) {
            log.info("Error at post mapping /create_credentials:" + errors.toString());
            return "create_credentials";
        }

        Person person = null;
        log.info("person id" + Long.toString(personAndUserCredentials.getId()));
        try {
            // List<PersonPermission> permissions =
            // permissionRepository.findPermissionByPersonId(personAndUserCredentials.getId());
            Client client = clientRepository.findByPersonId(personAndUserCredentials.getId()).get(0);
            person = personService.findByEmailAndClientId(personAndUserCredentials.getEmail(), client.getId());
        } catch (Error e) {
            log.info(e.toString());
            return "verify_pin";
        }

        if (person == null) {
            log.info("Person is null at create_credentials post mapping.");
            return "create_credentials";
        } else {
            log.info("This is the person at create_credentials: {}", person.toString());
        }

        User newUser = new User(personAndUserCredentials.getUsername(),
                personAndUserCredentials.getPassword(),
                personAndUserCredentials.getEmail());

        newUser.setPerson(person);
        newUser.setPasswordConfirm(personAndUserCredentials.getPassword());
        userService.create(userService.addRole(newUser, RoleConstants.user));

        // parse birthdate String for saving the person
        String localDateFormat = messageSource.getMessage("root.localDateFormat", null,
                LocaleContextHolder.getLocale());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(localDateFormat);
        LocalDate birthdate = LocalDate.parse(personAndUserCredentials.getBirthdateString(), formatter);

        person.setPin(null);
        person.setStatus(RegistrationStatus.APPROVED);
        person.setBirthdate(birthdate);
        personService.save(person);

        // after password reset, redirect to login page and login
        String message = messageSource.getMessage("root.login.profileUpdateSuccess", null, locale);
        redirectAttributes.addFlashAttribute("success", message);
        model.addAttribute("userC", newUser);

        return "redirect:/login";
    }

    /**
     * 
     */
    @GetMapping("/registration")
    public String registration(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("person", new PersonRegisterViewModel());
        return "registration";
    }

    /**
     * 
     */
    @PostMapping("/registration")
    public String registration(Model model,
            @Validated @ModelAttribute("user") User user,
            Errors userErrors,
            @Validated @ModelAttribute("person") PersonRegisterViewModel personViewModel,
            Errors personErrors,
            Locale locale) {
        if (userErrors.hasErrors() || personErrors.hasErrors()) {
            log.debug("Validation error after /registration: " + userErrors.toString() + personErrors.toString());
            return "registration";
        }

        // Nach der Validation sollte der "BirthdateString" String ein valides Datum
        // enthalten
        String localDateFormat = messageSource.getMessage("root.localDateFormat", null, locale);
        personViewModel.saveBirthdateStringToModel(localDateFormat);
        Person person = new Person(personViewModel); // Das ist notwendig weil Hibernate auf den Referenzen arbeitet und
                                                     // nach einem Cast immer noch
                                                     // die Subklasse erkennet (PersonAddPersonViewModel) und dann einen
                                                     // Fehler produziert.

        person = personService.save(person);
        if (person == null) {
            String message = messageSource.getMessage("root.registration.personNotSaved", null, locale);
            model.addAttribute("globalErrors", message);
            return "registration";
        }
        user.setPerson(person);

        String password = user.getPassword();
        userService.addRole(user, RoleConstants.user);
        user = userService.create(user);
        if (user == null) {
            personService.deleteById(person.getId());

            String message = messageSource.getMessage("root.registration.personNotSaved", null, locale);
            model.addAttribute("globalErrors", message);
            return "registration";
        }

        securityService.autoLogin(user.getUsername(), password);

        return "redirect:/user/client_login";
    }

    /**
     * get the forgot password form
     */
    @GetMapping("/forgot_password")
    public String showForgotPasswordForm(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("person", new UserPasswordResetViewModel());
        return "forgot_password";
    }

    /**
     * send reset password email
     */
    @PostMapping("/forgot_password")
    public String processForgotPassword(HttpServletRequest request,
            Model model,
            Locale locale) {

        String email = request.getParameter("email");
        String token = RandomString.make(30);
        User user = null;
        try {
            user = userRepository.findByEmail(email).orElse(null);
            model.addAttribute("user", new User());
        } catch (NonUniqueResultException e) {
            log.info("NonUniqueResultException at /forgot_password. Email is not unique.");
        }

        if (user != null) {
            userService.updateResetPasswordToken(token, email);
            String resetPasswordLink = Utility.getSiteURL(request) + "/reset_password?token=" + token;
            emailService.sendPasswordResetMail(email, resetPasswordLink);
            String message = messageSource.getMessage("root.user.change_password.emailSent", null, locale);
            model.addAttribute("success", message);
            return "forgot_password_message";
        } else {
            String message = messageSource.getMessage("root.user.change_password.emailSent", null, locale);
            model.addAttribute("success", message);
            return "forgot_password_message";
        }
    }

    /**
     * get the rest password form
     */
    @GetMapping("/reset_password")
    public String showResetPasswordForm(@Param(value = "token") String token,
            Model model, Locale locale) {
        User user = userService.findByToken(token);
        model.addAttribute("token", token);
        model.addAttribute("user", user);
        if (user != null && !user.getResetPasswordToken().equals("")) {
            return "reset_password";
        } else {
            String message = messageSource.getMessage("root.login.resetPasswordError", null, locale);
            model.addAttribute("invalidToken", message);
            return "reset_password_message";
        }
    }

    /**
     * send reset password email
     */
    @PostMapping("/reset_password")
    public String processResetPassword(HttpServletRequest request,
            Model model,
            RedirectAttributes redirectAttributes,
            Locale locale) {

        String token = request.getParameter("token");
        String password = request.getParameter("password");

        User user = userService.findByToken(token);

        if (user != null) {
            model.addAttribute("user", user);
            model.addAttribute("token", token);
            userService.updatePassword(user, password);
            String message = messageSource.getMessage("root.login.resetPasswordSuccess", null, locale);
            model.addAttribute("success", message);
            return "reset_password_message";
        }
        return "reset_password";
    }
}