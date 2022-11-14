package com.klix.backend.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.LongFunction;

import com.klix.backend.model.Client;
import com.klix.backend.model.GalleryPicture;
import com.klix.backend.model.Person;
import com.klix.backend.model.User;
import com.klix.backend.repository.ClientRepository;
import com.klix.backend.repository.ClientRoleRepository;
import com.klix.backend.repository.GalleryPictureRepository;
import com.klix.backend.repository.IdPictureRepository;
import com.klix.backend.repository.PermissionRepository;
import com.klix.backend.repository.PersonRepository;
import com.klix.backend.repository.PublicationResponseRepository;
import com.klix.backend.repository.UserRepository;
import com.klix.backend.repository.projections.PersonPermission;
import com.klix.backend.service.ClientService;
import com.klix.backend.service.PermissionService;
import com.klix.backend.service.PersonService;
import com.klix.backend.service.EmailService;
import com.klix.backend.service.user.UserService;
import com.klix.backend.service.ViewAccessService;
import com.klix.backend.service.interfaces.SecurityService;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.ui.Model;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BaseController {
    @Autowired
    protected PermissionRepository permissionRepository;
    @Autowired
    protected ClientRoleRepository clientRoleRepository;
    @Autowired
    protected ClientRepository clientRepository;
    @Autowired
    protected GalleryPictureRepository galleryPictureRepository;
    @Autowired
    protected UserService userService;
    @Autowired
    protected ViewAccessService viewAccessService;
    @Autowired
    protected ClientService clientService;
    @Autowired
    protected PersonService personService;
    @Autowired
    protected EmailService emailService;
    @Autowired
    protected PermissionService permissionService;
    @Autowired
    protected SecurityService securityService;

    @Autowired
    protected MessageSource messageSource;

    @Autowired
    protected IdPictureRepository idPictureRepository;
    @Autowired
    protected PersonRepository personRepository;
    @Autowired
    protected UserRepository userRepository;
    @Autowired
    protected PublicationResponseRepository publicationResponseRepository;

    protected User findLoggedUser(Locale locale) throws NoSuchElementException {
        User user = securityService.findLoggedInUser();

        if (user != null && user.getPerson() != null)
            return user;
        if (user == null)
            throw new NoSuchElementException(messageSource.getMessage("root.userMissing", null, locale));
        if (user.getPerson() == null)
            throw new NoSuchElementException(messageSource.getMessage("root.personMissing", null, locale));

        throw new NoSuchElementException(
                messageSource.getMessage("root.dev.developController.uploadError", null, locale));
    }

    protected boolean hasPersonPermissionForClient(long personId, long clientId, long[] clientRoleIds) {
        if (!checkPersonId(personId) || !checkClientId(clientId)) {
            return false;
        }
        List<PersonPermission> permissions = permissionRepository.findPermissionsByPersonAndClient(personId, clientId);

        for (PersonPermission permission : permissions) {
            if (ArrayUtils.contains(clientRoleIds, permission.getClient_role_id())) {
                return true;
            }
        }
        return false;
    }

    protected boolean isGalleryPictureOfClient(long galleryPictureId, long clientId) {
        if (!checkGalleryPictureId(galleryPictureId) || !checkClientId(clientId)) {
            return false;
        }

        Person uploader = galleryPictureRepository.getOne(galleryPictureId).getUploader();
        List<PersonPermission> uploaderPermissions = permissionRepository
                .findPermissionsByPersonAndClient(uploader.getId(), clientId);
        for (PersonPermission pp : uploaderPermissions) {
            if (pp.getClient_role_id() == 2L || pp.getClient_role_id() == 3L || pp.getClient_role_id() == 4L) {
                return true;
            }
        }
        return false;
    }

    protected boolean checkGalleryPictureId(long id) {
        LongFunction<GalleryPicture> lookupFunction = (long picId) -> galleryPictureRepository.findById(picId)
                .orElse(null);
        return this.checkIds(new long[] { id }, lookupFunction);
    }

    protected boolean checkGalleryPictureIds(long[] ids) {
        LongFunction<GalleryPicture> lookupFunction = (long id) -> galleryPictureRepository.findById(id).orElse(null);
        return this.checkIds(ids, lookupFunction);
    }

    protected boolean checkUserId(long id) {
        LongFunction<User> lookupFunction = (long userId) -> userService.findById(userId);
        return this.checkIds(new long[] { id }, lookupFunction);
    }

    protected boolean checkUserIds(long[] ids) {
        LongFunction<User> lookupFunction = (long id) -> userService.findById(id);
        return this.checkIds(ids, lookupFunction);
    }

    protected boolean checkClientId(long id) {
        LongFunction<Client> lookupFunction = (long clientId) -> clientService.findById(clientId);
        return this.checkIds(new long[] { id }, lookupFunction);
    }

    protected boolean checkClientIds(long[] ids) {
        LongFunction<Client> lookupFunction = (long id) -> clientService.findById(id);
        return this.checkIds(ids, lookupFunction);
    }

    protected boolean checkPersonId(Long id) {
        LongFunction<Person> lookupFunction = (long personId) -> personService.findById(personId);
        // !TO DO: instead of findBy...() we could use existsBy...() since this
        // decreases the response time.(Latency)
        return this.checkIds(new long[] { id }, lookupFunction);
    }

    protected boolean checkPersonIds(long[] ids) {
        LongFunction<Person> lookupFunction = (long id) -> personService.findById(id);
        return this.checkIds(ids, lookupFunction);
    }

    protected boolean checkPermissionId(long id) {
        LongFunction<PersonPermission> lookupFunction = (long permissionId) -> permissionService.findById(permissionId);
        return this.checkIds(new long[] { id }, lookupFunction);
    }

    protected boolean checkPermissionIds(long[] ids) {
        LongFunction<PersonPermission> lookupFunction = (long id) -> permissionService.findById(id);
        return this.checkIds(ids, lookupFunction);
    }

    private <T> boolean checkIds(long[] ids, LongFunction<T> lookupFunction) {
        StackTraceElement[] trace = Thread.currentThread().getStackTrace();
        String errorMessage = (trace.length >= 3 ? trace[2].getMethodName() : "some check") + "() returns false!";
        return checkIds(ids, lookupFunction, errorMessage);
    }

    private <T> boolean checkIds(long[] ids, LongFunction<T> lookupFunction, String errorMessage) {
        for (long id : ids) {
            T instance = lookupFunction.apply(id);

            // validity checks
            if (instance == null) {
                log.warn(errorMessage);
                return false;
            }
        }
        return true;
    }

    protected void addClientInfo(Model model, Long personId) {
        List<PersonPermission> permissions = permissionRepository.findPermissionByPersonId(personId);

        List<PersonPermission> completePermissions = new ArrayList<>();
        for (PersonPermission p : permissions) {
            if (p.getId() != null && p.getClient_id() != null && p.getClient_role_id() != null)
                completePermissions.add(p);
        }

        model.addAttribute("clientsInfo", completePermissions);
        Person personInfo = personService.findById(personId);
        model.addAttribute("pname", fullNameFormatter.apply(personInfo.getFirstName(), personInfo.getLastName()));

    }

    private BinaryOperator<String> fullNameFormatter = (firstName, lastName) -> firstName + " " + lastName;

    protected List<PersonPermission> getPermissions(Long personId) {
        List<PersonPermission> permissions = permissionRepository.findPermissionByPersonId(personId);

        List<PersonPermission> completePermissions = new ArrayList<>();
        for (PersonPermission p : permissions) {
            if (p.getId() != null && p.getClient_id() != null && p.getClient_role_id() != null)
                completePermissions.add(p);
        }

        return completePermissions;
    }
}
