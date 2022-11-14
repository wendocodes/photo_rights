package com.klix.backend.controller.client;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.validation.Valid;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.klix.backend.controller.BaseImageController;
import com.klix.backend.enums.FaceFrameGenerationStatus;
import com.klix.backend.enums.GalleryImage_DetectionStatus;
import com.klix.backend.enums.GalleryPictureIdentificationStatus;

import com.klix.backend.enums.PublicationRequestStatus;
import com.klix.backend.enums.PublicationResponseStatus;
import com.klix.backend.enums.ViewAccess;
import com.klix.backend.model.GalleryPicture;
import com.klix.backend.model.GalleryPictureFaceFrame;
import com.klix.backend.model.Groups;
import com.klix.backend.model.IdPicture;
import com.klix.backend.model.Client;
import com.klix.backend.viewmodel.AdditionalGuardianViewModel;
import com.klix.backend.viewmodel.ChildGuardianRegistrationViewModel;
import com.klix.backend.viewmodel.ChildRegistrationViewModel;
import com.klix.backend.viewmodel.MitarbeiterGalleryViewModel;
import com.klix.backend.viewmodel.StaffGroupViewModel;

import com.klix.backend.model.Person;
import com.klix.backend.model.PublicationRequest;
import com.klix.backend.model.PublicationResponse;
import com.klix.backend.repository.projections.PersonPermission;
import com.klix.backend.service.IdentificationService;
import com.klix.backend.service.picture.PictureFunctionService;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.*;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/client/*")
public class ClientController extends BaseImageController {

        @GetMapping("/{id}")
        public String resolvePermission(Model model, @PathVariable("id") long clientPersonRoleId) {
                // ClientPersonRoleId is the unique id to PersonPermission
                model.addAttribute("id", clientPersonRoleId);
                PersonPermission permission = permissionRepository.findPermissionById(clientPersonRoleId).orElse(null);
                if (permission == null) {
                        log.error("PersonPermission with id not found in resolvePermission");
                        return "client/";
                }
                return "redirect:/client/" + permission.getClient_id() + "/role/" + permission.getClient_role_id();
        }

        /**
         * Page for client_role 'Administrator'
         */
        @GetMapping("{clientId}/role/1")
        public String getMAdminSite(@PathVariable("clientId") long clientId, Model model) {
                if (!checkClientId(clientId)) {
                        log.error("Client with id not found in getMAdminSite");
                        return "client/";
                }
                Client client = clientService.findById(clientId);
                model.addAttribute("client", client);
                model.addAttribute("rolename", "Administrator");
                return "client/client_role";
        }

        /**
         * Gallery page for 'Mitarbeiter'
         */
        @Transactional

        @GetMapping("{clientId}/role/{roleId}/gallery")

        public String getGalleryPage(@PathVariable("clientId") long clientId, @PathVariable("roleId") long roleId,
                        Model model, Locale locale) {
                ViewAccess viewAccess = viewAccessService.checkPermissions(clientId, "client/mitarbeiter/gallery");
                Long personId = personService.getPersonId();
                model.addAttribute("id", personId);
                if (viewAccess == ViewAccess.FORBIDDEN || !checkClientId(clientId) || personId == null) {
                        return "errors/access-denied";
                }
                List<PersonPermission> permissions = getPermissions(personId);

                model.addAttribute("clientsInfo", permissions);

                PersonPermission permission = permissions.stream()

                                .filter(p -> p.getClient_role_id() == 2 || p.getClient_role_id() == 1)

                                .findFirst().orElse(null);

                model.addAttribute("permission", permission);

                if (permission == null) {
                        log.info("PersonPermission is null at clientController. Access denied.");
                        return "errors/access-denied";
                }
                Client client = clientService.findById(clientId);

                List<Groups> groups = groupsRepository.findByClient(client);
                // find PersonPermissions of persons with same client and role of id 2
                // (Mitarbeiter) or 3 (Benutzer) or 4 (Jungbenutzer)
                List<PersonPermission> personPermissions = permissionRepository
                                .findPermissionsByRolesAndByClientOrderByPersonId(Arrays.asList(2l, 3l, 4l), clientId);
                MitarbeiterGalleryViewModel viewModel = computeViewModel(client, personPermissions);
                // for status tracking of pubRequest
                model.addAttribute("PublicationResponseStatus", PublicationResponseStatus.class);
                // if no images found
                model.addAttribute("noImages",
                                messageSource.getMessage("root.client.mitarbeiter.gallery.noImages", null, locale));
                model.addAttribute("noImages",
                                messageSource.getMessage("root.client.mitarbeiter.gallery.noImages", null, locale));
                model.addAttribute("roleId", String.valueOf(roleId));
                model.addAttribute("clientId", String.valueOf(clientId));
                model.addAttribute("data", viewModel);
                model.addAttribute("groups", groups);
                model.addAttribute("personpermission", permission);
                return "client/mitarbeiter/gallery";
        }

        private MitarbeiterGalleryViewModel computeViewModel(Client client, List<PersonPermission> personPermissions) {

                MitarbeiterGalleryViewModel viewModel = new MitarbeiterGalleryViewModel();

                viewModel.setClient(client);

                for (PersonPermission pp : personPermissions) {
                        List<GalleryPicture> uploadedPictures = galleryPictureRepository
                                        .findByUploaderId(pp.getPerson_id());
                        // Mitarbeiter or Benutzer or Jungbenutzer of permission did not upload any
                        // image. Stop here
                        if (uploadedPictures.isEmpty())
                                continue;
                        // gather properties of person who uploaded image(s)
                        Person person = personRepository.getOne(pp.getPerson_id());
                        Set<String> roles = new HashSet<>();
                        MitarbeiterGalleryViewModel.UploaderSection section = viewModel
                                        .addUploader(viewModel.new Uploader(person, roles));
                        section.getUploader().addRoleName(pp.getClient_role_name());
                        // gather further information to each image the person uploaded
                        for (GalleryPicture pic : uploadedPictures) {

                                // init place for image and add it along with all recognized persons on it
                                MitarbeiterGalleryViewModel.Image image = viewModel.new Image();
                                image.setPicture(pic);
                                List<Long> recognizedPersonIds = computeRecognizedPersonsIds(pic.getId());
                                image.setRecognizedPersons(personRepository.findAllById(recognizedPersonIds));
                                // get related PublicationRequest of image
                                Set<PublicationRequest> pubRequests = this.publicationRequestRepository
                                                .findByClientIdAndGalleryPictureId(client.getId(), pic.getId());
                                if (pubRequests.size() > 1) {
                                        log.warn("Multiple PublicationRequests for Client and Picture found! Should be 1 or 0.");
                                }
                                PublicationRequest pubRequest = pubRequests.isEmpty() ? null
                                                : pubRequests.toArray(new PublicationRequest[1])[0];
                                image.setPubRequest(pubRequest);

                                // if there exists a PublicationRequest already, collect its responses
                                Map<String, List<String>> personNameByResponseStatus = new HashMap<>();

                                personNameByResponseStatus.put("PENDING", new ArrayList<>());

                                personNameByResponseStatus.put("ALLOWED", new ArrayList<>());

                                personNameByResponseStatus.put("FORBIDDEN", new ArrayList<>());
                                if (pubRequest != null) {
                                        for (PublicationResponse pr : publicationResponseRepository
                                                        .findByPublicationRequestId(pubRequest.getId())) {
                                                List<String> personsFromMap = personNameByResponseStatus.getOrDefault(
                                                                pr.getStatus().name(),
                                                                new ArrayList<>());
                                                Person p = personRepository.getOne(pr.getPersonId());

                                                personsFromMap.add(p.getFirstName() + ", " + p.getLastName());

                                                personNameByResponseStatus.put(pr.getStatus().name(), personsFromMap);
                                        }
                                        image.setPersonNameByResponseStatus(personNameByResponseStatus);

                                        image.setPubResponses(publicationResponseRepository
                                                        .findByPublicationRequestId(pubRequest.getId()));

                                }
                                // load associated model instances to be able to work in view
                                image.getPubStatistic();
                                // add it to display in view
                                section.addImage(image);
                        }
                }
                return viewModel;
        }

        @Transactional(value = TxType.REQUIRES_NEW)
        @PostMapping("{clientId}/role/{roleId}/{status}")
        public String togglePublicationRequests(@ModelAttribute("id") long id,
                        @ModelAttribute("text") String text,
                        @PathVariable("clientId") long clientId,
                        @PathVariable("roleId") long roleId,
                        @PathVariable("status") String status) {
                String redirectUrl = "redirect:/client/" + clientId + "/role/2/gallery";

                // check if image exists & if image with id 'id' belongs to client with id
                // 'clientId'
                if (!checkGalleryPictureId(id) || !isGalleryPictureOfClient(id, clientId)) {
                        // TODO: better error handling
                        log.warn("in togglePublicationRequests(): image is not found in client");
                        return redirectUrl;
                }

                // if PublicationRequest with imageId == id exists: delete it completely
                Set<PublicationRequest> requests = this.publicationRequestRepository.findByClientIdAndGalleryPictureId(
                                clientId,
                                id);
                log.info("Requests: " + requests);

                if (status.equals("request")) {
                        if (requests.isEmpty()) {

                                PublicationRequest request = new PublicationRequest(clientId, id, text);
                                request = this.publicationRequestRepository.save(request);
                                requests.add(request);
                                log.info("Request without brackets: {}", request);
                        }
                        for (PublicationRequest request : requests) {
                                request.setText(text);
                                request.setStatus(PublicationRequestStatus.REQUESTED);
                        }
                        this.publicationRequestRepository.saveAll(requests);
                        for (PublicationRequest request : requests) {
                                List<Long> recognizedPersonIds = computeRecognizedPersonsIds(id);

                                for (long personId : recognizedPersonIds) {
                                        PublicationResponse response = createPublicationResponse(request.getId(),
                                                        personId);
                                        PublicationResponse saved = publicationResponseRepository.save(response);
                                        log.info("response : {}", saved);
                                }
                        }

                } else if (status.equals("revoke") && !requests.isEmpty()) {
                        for (PublicationRequest request : requests) {
                                request.setText(null);
                                request.setStatus(PublicationRequestStatus.REVOKED);
                                log.info("add image to detection table: {}", request);
                                // this is the place we need to save the image back in the detection table
                                Set<PublicationResponse> responses = this.publicationResponseRepository
                                                .findByPublicationRequestId(request.getId());
                                this.publicationResponseRepository.deleteAll(responses);
                        }
                        this.publicationRequestRepository.saveAll(requests);
                } else {
                        log.warn("invalid operation");
                }

                return redirectUrl;
        }

        /**
         * @author Andreas
         * @since 28.05.2022
         */
        /*
         * Eine eigene Methode dafür ist sinnvoll, weil Verantwortliche für die
         * Erlaubnis zugeteilt werden
         * müssen und wer weiß, was später dazu kommt.
         * Woher wissen wir, dass eine Person nicht über sich selber entscheiden darf,
         * sondern einen
         * Sorgeberechtigten benötigt? Aktuell wird dies in dieser Methode darüber
         * entschieden, ob die Person
         * einen legal guardian hat oder nicht. Das ist zur Zeit (!) unproblematisch, da
         * man zur Zeit kein Kind
         * anmelden kann, ohne einen legal Guardian mit anzumelden. Wer weiß, ob das mal
         * anders sein wird.
         * Ich habe früher schon darüber berichtet, dass die Erlaubnis gebende Person
         * nicht mit der Person auf dem
         * Bild übereinstimmen muss und Vorschläge gemacht. Allerdings kann ich das bis
         * zum Testlauf in wenigen
         * Tagen nicht alles umbauen.
         * Problematisch wird es auch, wenn irgendwann mal ein Kind erst angemeldet wird
         * und der erste
         * Sorgeberechtigte später. Das ist alles nicht frühzeitig durchdacht worden.
         */
        public PublicationResponse createPublicationResponse(Long publicationRequestID, Long recognizedPersonID) {

                Person recognizedPerson = personRepository.findById(recognizedPersonID).orElse(null);
                PublicationResponse publicationResponse = new PublicationResponse(publicationRequestID,
                                recognizedPersonID);
                Set<Person> legalGuardians = recognizedPerson.getLegalGuardians();
                if (legalGuardians != null && legalGuardians.size() != 0) {// Hier ist im Grunde die Entscheidung, bin
                                                                           // ich Kind
                                                                           // oder Erwachsener
                        publicationResponse.addResponsiblePersonIds(legalGuardians.stream().map(p -> p.getId())
                                        .collect(Collectors.toSet()));
                } else {
                        publicationResponse.addResponsiblePersonId(recognizedPersonID);
                }

                return publicationResponse;
        }

        /**
         * Page for client_role 'Benutzer'
         */
        @GetMapping("{clientId}/role/3")
        public String getBenutzerSite(@PathVariable("clientId") long clientId, Model model) {
                if (!checkClientId(clientId)) {
                        log.error("Client with id not found in getBenutzerSite");
                        return "client/";
                }
                Client client = clientService.findById(clientId);
                model.addAttribute("client", client);
                model.addAttribute("rolename", "Benutzer");
                return "client/client_role";
        }

        /**
         * Page for client_role 'Jungbenutzer'
         */
        @GetMapping("{clientId}/role/4")
        public String getJungbenutzerSite(@PathVariable("clientId") long clientId, Model model) {
                if (checkClientId(clientId)) {
                        log.error("Client with id not found in getJungbenutzerSite");
                        return "client/";
                }
                Client client = clientService.findById(clientId);
                model.addAttribute("client", client);
                model.addAttribute("rolename", "Jungbenutzer");
                return "client/client_role";
        }

        /**
         * render dashboard in view
         */
        @GetMapping("{clientId}/dashboard/role/{roleId}")
        public String getclientLinksPage(@PathVariable("clientId") long clientId,
                        @PathVariable("roleId") long roleId,
                        Model model) {
                ViewAccess viewaccess = viewAccessService.checkPermissions(clientId, "client/dashboard");
                if (viewaccess == ViewAccess.FORBIDDEN) {
                        log.warn("This user does not have permissions to view client pages");
                        return "errors/access-denied";
                }

                Long personId = personService.getPersonId();
                this.addClientInfo(model, personId);

                model.addAttribute("clientId", clientId);
                model.addAttribute("roleId", roleId);
                // Fetch Dashboard notifications
                Set<PublicationResponse> response = this.publicationResponseRepository
                                .findByStatusAndPersonId(PublicationResponseStatus.ALLOWED, personId);
                List<GalleryPicture> detected = this.galleryPictureRepository
                                .findByDetectionStatus(GalleryImage_DetectionStatus.neverReviewed.name());
                List<GalleryPicture> identification_status = this.galleryPictureRepository
                                .findByIdentificationStatus(
                                                GalleryPictureIdentificationStatus.EMPLOYEES_NOT_FINISHED.name());

                int number_face_frames = 0;
                for (GalleryPicture galleryImage : identification_status) {
                        number_face_frames += this.galleryPictureFaceFrameRepository
                                        .findByIdentificationStatus(3, galleryImage.getId()).size();
                }
                model.addAttribute("pubResponse", response.size());
                model.addAttribute("cnnPhoto", detected.size());
                model.addAttribute("identification_status", number_face_frames);

                IdPicture loadedImage = idPictureRepository.findByPersonId(personId).orElse(null);
                model.addAttribute("image", loadedImage);
                model.addAttribute("id", personId);
                PersonPermission permission = permissionRepository
                                .findPermissionByPersonAndClientAndRole(personId, clientId, roleId).orElse(null);
                model.addAttribute("permission", permission);
                if (permission == null) {
                        return "errors/access-denied";
                }
                return "client/dashboard";
        }

        /**
         * render groups in view
         */
        @GetMapping("{clientId}/role/{roleId}/groups")
        public String getgroupsPage(@PathVariable("clientId") long clientId, @PathVariable("roleId") long roleId,
                        Model model) {
                if (!checkClientId(clientId)) {
                        log.warn("Client not found in getgroupsPage");
                        return "errors/access-denied";
                }

                ViewAccess viewaccess = viewAccessService.checkPermissions(clientId, "client/mitarbeiter/groups");
                if (viewaccess == ViewAccess.FORBIDDEN) {
                        log.info("this user does not have the permissions to view client/mitarbeiter/groups");
                        return "errors/access-denied";
                }

                Long personId = personService.getPersonId();
                model.addAttribute("id", personId);

                PersonPermission permission = permissionRepository
                                .findPermissionByPersonAndClientAndRole(personId, clientId, roleId).orElse(null);
                // check if the permissions object null and deny access
                if (permission == null) {
                        log.warn("This user (person " + personId
                                        + ") does not have permission to access the groups of client"
                                        + clientId + " with role" + roleId + ".");
                        return "errors/access-denied";
                }

                // the supervisor should only see the groups that he/she is responsible for
                Client client = clientService.findById(clientId);
                List<Groups> groups = groupsRepository.findByClient(client);

                model.addAttribute("groups", groups);
                model.addAttribute("permission", permission);
                model.addAttribute("clientId", clientId);
                model.addAttribute("roleId", roleId);
                model.addAttribute("clientroleid", roleId);

                return "client/mitarbeiter/groups";
        }

        /**
         * Table view for group supervisors
         */
        @GetMapping("{clientId}/role/{roleId}/staff_table")
        public String client_group_supervisors(@PathVariable("clientId") long clientId,
                        @PathVariable("roleId") long roleId,
                        Model model, Locale locale) {
                if (!checkClientId(clientId)) {
                        return "errors/access-denied";
                }

                ViewAccess viewaccess = viewAccessService.checkPermissions(clientId, "client/clientadmin/staff_table");
                if (viewaccess == ViewAccess.FORBIDDEN) {
                        return "errors/access-denied";
                }

                Long personId = personService.getPersonId();
                model.addAttribute("id", personId);

                PersonPermission permission = permissionRepository
                                .findPermissionByPersonAndClientAndRole(personId, clientId, roleId).orElse(null);
                if (permission == null) {
                        return "errors/access-denied";
                }

                // get all current supervisors for the client
                List<Map<String, Object>> supervisorInfo = new ArrayList<>();
                List<Groups> groups = groupsRepository.findByClient(clientService.findById(clientId));
                for (Groups group : groups) {
                        for (Person supervisor : group.getSupervisors()) {
                                supervisorInfo.add(
                                                Map.of(
                                                                "id", "" + supervisor.getId(),
                                                                "firstName", supervisor.getFirstName(),
                                                                "lastName", supervisor.getLastName(),
                                                                "email",
                                                                Optional.ofNullable(supervisor.getEmail()).orElse(""),
                                                                "groupName", group.getGroupName(),
                                                                "groupId", group.getId()));
                        }
                }

                // get all potential supervisors for the client
                Set<Person> supervisors = new HashSet<>();
                List<PersonPermission> supervisorPermissions = permissionRepository
                                .findPermissionsByRolesAndByClient(Arrays.asList(1L, 2L), clientId);
                for (PersonPermission supervisorPermission : supervisorPermissions) {
                        Person supervisor = personRepository.findById(supervisorPermission.getPerson_id()).orElse(null);
                        if (supervisor != null) {
                                supervisors.add(supervisor);
                        }
                }

                model.addAttribute("title",
                                messageSource.getMessage("root.client.mitarbeiter.gallery.groupTitle", null, locale));
                model.addAttribute("add Supervisor",
                                messageSource.getMessage("root.add_person.editHeaderLabel", null, locale));
                model.addAttribute("groups", groups);
                model.addAttribute("supervisors", supervisors);
                model.addAttribute("supervisorInformation", supervisorInfo);
                model.addAttribute("supervisor", new StaffGroupViewModel());
                model.addAttribute("clientId", clientId);
                model.addAttribute("clientroleid", roleId);
                model.addAttribute("permission", permission);
                model.addAttribute("columns", new String[][] {
                                new String[] { messageSource.getMessage("root.add_person.firstNameLabel", null, locale),
                                                "firstName" },
                                new String[] { messageSource.getMessage("root.add_person.lastNameLabel", null, locale),
                                                "lastName" },
                                new String[] { messageSource.getMessage("root.add_person.emailLabel", null, locale),
                                                "email" },
                                new String[] { messageSource.getMessage("root.add_person.groupNameLabel", null, locale),
                                                "groupName" },
                                new String[] { messageSource.getMessage("root.add_person.editHeaderLabel", null,
                                                locale), "edit" },
                                new String[] { messageSource.getMessage("root.add_person.deleteHeaderLabel", null,
                                                locale), "delete" },
                });

                return "client/clientadmin/staff_table";
        }

        /**
         * delete group supervisor by Id. Removes the mapping between Group and
         * Supervisor
         */
        @GetMapping("{clientId}/role/{roleId}/delete_supervisor/{supervisorId}/group/{groupId}")
        public String deleteSupervisorById(@PathVariable("clientId") long clientId,
                        @PathVariable("roleId") long roleId,
                        @PathVariable("supervisorId") long supervisorId,
                        @PathVariable("groupId") long groupId,
                        Model model, Locale locale) {
                // check if logged in person has permission to access
                if (roleId != 1 || !hasPersonPermissionForClient(personService.getPersonId(), clientId,
                                new long[] { 1L })) {
                        return "errors/access-denied";
                }

                Groups groups = groupsRepository.findById(groupId).orElse(null);
                if (groups == null || groups.getClient().getId() != clientId) {
                        return this.client_group_supervisors(clientId, roleId, model, locale);
                }

                for (Person supervisor : groups.getSupervisors()) {
                        if (supervisor.getId() == supervisorId) {
                                log.info("Found Supervisor to delete :{}", supervisor);
                                groups.getSupervisors().remove(supervisor);
                                break;
                        }
                }
                groupsService.save(groups);

                // after delete,update and return to staff table
                return this.client_group_supervisors(clientId, roleId, model, locale);
        }

        /**
         * assign a group supervisor to a group
         */
        @GetMapping("{clientId}/role/{roleId}/map_supervisor")
        public String mapSupervisorById(@PathVariable("clientId") long clientId,
                        @PathVariable("roleId") long roleId,
                        @RequestParam(name = "supervisorId") long supervisorId,
                        @RequestParam(name = "groupId") long groupId,
                        Model model, Locale locale) {
                // check if logged in person has permission to access
                if (roleId != 1 || !hasPersonPermissionForClient(personService.getPersonId(), clientId,
                                new long[] { 1L })) {
                        return "errors/access-denied";
                }

                Groups groups = groupsService.findById(groupId);
                Person supervisor = personService.findById(supervisorId);
                log.info("Groups : {} and Supervisors : {}", groups, supervisorId);

                // check if ...
                if (groups == null || supervisor == null || // ... group & supervisor exist
                                groups.getClient().getId() != clientId || // ... group belongs to client
                                !hasPersonPermissionForClient(supervisorId, groups.getClient().getId(),
                                                new long[] { 1L, 2L })) // ...
                                                                        // supervisor
                                                                        // belongs
                                                                        // to
                                                                        // client
                                                                        // of
                                                                        // group
                {
                        return this.client_group_supervisors(clientId, roleId, model, locale);
                }

                groups.getSupervisors().add(supervisor);
                log.info("Groups  : {}", groups);
                groupsService.save(groups);

                // after mapping and saving, return to staff table
                return this.client_group_supervisors(clientId, roleId, model, locale);
        }

        /**
         * Edit supervisor associated to a group
         */
        @PostMapping("{clientId}/role/{roleId}/person_edit/{groupId}/{supervisorId}")
        public String update_supervisor(@PathVariable("clientId") long clientId,
                        @PathVariable("roleId") long roleId,
                        @PathVariable("groupId") long groupId,
                        @PathVariable("supervisorId") long supervisorId,
                        @ModelAttribute("supervisor") StaffGroupViewModel viewModel,
                        Model model, Locale locale) {
                // check if logged in person has permission to access
                if (roleId != 1 || !hasPersonPermissionForClient(personService.getPersonId(), clientId,
                                new long[] { 1L })) {
                        return "errors/access-denied";
                }

                Groups group = groupsService.findById(groupId);
                // update group name
                if (group != null) {
                        group.setGroupName(viewModel.getGroupName());
                        groupsService.save(group);
                }
                // update person details
                personService.supervisor_person_edit(supervisorId, viewModel.getFirstName(), viewModel.getLastName(),
                                viewModel.getEmail());

                // redirect to table view of person
                return this.client_group_supervisors(clientId, roleId, model, locale);
        }

        /**
         * View Model for registering a child
         */
        @GetMapping("{clientId}/role/{roleId}/register_child")
        public String register_child(Model model, Locale locale,
                        @PathVariable("clientId") long clientId,
                        @PathVariable("roleId") long roleId) {

                Long person_id = personService.getPersonId();
                model.addAttribute("id", person_id);

                List<PersonPermission> permissions = getPermissions(person_id);
                model.addAttribute("clientsInfo", permissions);

                PersonPermission permission = permissions.stream()
                                .filter(p -> p.getClient_role_id() == 2 || p.getClient_role_id() == 1)
                                .findFirst().orElse(null);

                model.addAttribute("permission", permission);

                if (model.getAttribute("person") == null)
                        model.addAttribute("child", new ChildRegistrationViewModel());

                model.addAttribute("clientId", clientId);
                model.addAttribute("roleId", roleId);
                return "client/clientadmin/register_child";
        }

        /**
         * Add a Child: child is associated to parent person
         */
        @PostMapping("{clientId}/role/{roleId}/register_child")
        public String register_child(Model model, Locale locale,
                        @PathVariable("clientId") long clientId,
                        @PathVariable("roleId") long roleId,
                        @ModelAttribute("child") @Valid ChildRegistrationViewModel childViewModel,
                        Errors personErrors) {
                // on error: stay on page and display errors for a good user experience
                if (personErrors.hasErrors()) {
                        log.info(personErrors.toString());
                        model.addAttribute("child", childViewModel);

                        return "client/clientadmin/register_child";
                }

                // parse birthdate String for saving
                String localDateFormat = messageSource.getMessage("root.localDateFormat", null,
                                LocaleContextHolder.getLocale());
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(localDateFormat);
                LocalDate birthdate = LocalDate.parse(childViewModel.getChildBirthdate(), formatter);

                Person child = personService.createChild(childViewModel.getChildFirstName(),
                                childViewModel.getChildLastName(),
                                birthdate,
                                clientId);

                PersonPermission permission = permissionRepository
                                .findPermissionByPersonAndClientAndRole(child.getId(), clientId, roleId).orElse(null);
                model.addAttribute("permission", permission);
                model.addAttribute("clientId", clientId);
                model.addAttribute("childId", child.getId());
                model.addAttribute("person", childViewModel);
                model.addAttribute("roleId", roleId);

                return register_parent(model, locale, Integer.parseInt(childViewModel.getNumberLegalGuardian()),
                                child.getId(),
                                clientId, roleId);

        }

        /**
         * View Model for registering legal guardian
         */
        @GetMapping("{clientId}/role/{roleId}/register_parent")
        public String register_parent(Model model, Locale local,
                        @ModelAttribute("numberLegalGuardian") int numberLegalGuardian,
                        @ModelAttribute("childId") long childId,
                        @PathVariable("clientId") long clientId,
                        @PathVariable("roleId") long roleId) {

                Long person_id = personService.getPersonId();
                model.addAttribute("id", person_id);

                List<PersonPermission> permissions = getPermissions(person_id);
                model.addAttribute("clientsInfo", permissions);

                PersonPermission permission = permissions.stream()
                                .filter(p -> p.getClient_role_id() == 2 || p.getClient_role_id() == 1)
                                .findFirst().orElse(null);

                model.addAttribute("permission", permission);

                if (model.getAttribute("person") == null) {
                        log.info("addding new model");
                        ChildGuardianRegistrationViewModel childGuardianRegistrationViewModel = new ChildGuardianRegistrationViewModel();
                        childGuardianRegistrationViewModel.setNumberLegalGuardian(numberLegalGuardian);
                        model.addAttribute("person", childGuardianRegistrationViewModel);
                }

                model.addAttribute("clientId", clientId);
                model.addAttribute("roleId", roleId);
                model.addAttribute("childId", childId);
                model.addAttribute("numberLegalGuardian", numberLegalGuardian);

                return "client/clientadmin/register_parent";
        }

        /**
         * Add a legal guardian
         */
        @PostMapping("{clientId}/role/{roleId}/child/{childId}/register_parent")
        public String register_parent(Model model,
                        @PathVariable("clientId") long clientId,
                        @PathVariable("roleId") long roleId,
                        @PathVariable("childId") long childId,
                        @ModelAttribute("person") @Valid ChildGuardianRegistrationViewModel guardianRegistrationViewModel,
                        Errors personErrors, BindingResult result, RedirectAttributes atts, Locale locale) {
                // on error: stay on page and display errors for a good user experience
                if (personErrors.hasErrors()) {
                        log.info(personErrors.toString());
                        model.addAttribute("person", guardianRegistrationViewModel);

                        if (personErrors.getGlobalError() != null
                                        && personErrors.getGlobalError().getCode().equals("UniqueConstraint")) {
                                ObjectError emailError = personErrors.getGlobalError();
                                result.rejectValue("email", "error.user", emailError.getDefaultMessage());
                        }
                        return register_parent(model, locale, guardianRegistrationViewModel.getNumberLegalGuardian(),
                                        guardianRegistrationViewModel.getChildId(), clientId, roleId);
                }

                Person parent = personService.createParent(guardianRegistrationViewModel.getFirstName(),
                                guardianRegistrationViewModel.getLastName(),
                                guardianRegistrationViewModel.getEmail(),
                                clientId);

                personService.addGuardian(guardianRegistrationViewModel.getChildId(), parent);

                PersonPermission permission = permissionRepository
                                .findPermissionByPersonAndClientAndRole(parent.getId(), clientId, roleId).orElse(null);
                model.addAttribute("permission", permission);
                model.addAttribute("clientId", clientId);
                model.addAttribute("roleId", roleId);

                guardianRegistrationViewModel
                                .setNumberLegalGuardian(guardianRegistrationViewModel.getNumberLegalGuardian() - 1);

                if (guardianRegistrationViewModel.getNumberLegalGuardian() == 0) {
                        return "redirect:/client/" + clientId + "/role/" + roleId + "/registration_status_table";
                } else {
                        atts.addAttribute("childId", guardianRegistrationViewModel.getChildId());
                        atts.addAttribute("numberLegalGuardian",
                                        guardianRegistrationViewModel.getNumberLegalGuardian());
                        return "redirect:/client/" + clientId + "/role/" + roleId + "/register_parent";

                }

        }

        /**
         * @author Joyce
         * @since 06.05.2022
         */
        @PostMapping("{clientId}/role/{roleId}/add_guardian")
        public String addGuardian(Model model,
                        @PathVariable("clientId") long clientId,
                        @PathVariable("roleId") long roleId,
                        @ModelAttribute("guardian") @Valid AdditionalGuardianViewModel parentModel,
                        Errors parentErrors, BindingResult result, RedirectAttributes atts, Locale locale) {
                if (parentErrors.hasErrors()) {
                        log.info(parentErrors.toString());
                        if (parentErrors.getGlobalError() != null
                                        && parentErrors.getGlobalError().getCode().equals("UniqueConstraint")) {
                                ObjectError emailError = parentErrors.getGlobalError();
                                result.rejectValue("email", "error.user", emailError.getDefaultMessage());
                        }
                        atts.addAttribute("hasErrors", true);
                        model.addAttribute("guardian", parentModel);
                        return child_legal_guardian_table(model, locale, new ChildGuardianRegistrationViewModel(),
                                        clientId, roleId);
                }

                Person parent = personService.createParent(parentModel.getParentFirstName(),
                                parentModel.getParentLastName(),
                                parentModel.getEmail(), clientId);
                personService.addGuardian(parentModel.getId(), parent);
                model.addAttribute("clientId", clientId);
                model.addAttribute("roleId", roleId);

                return "redirect:/client/" + clientId + "/role/" + roleId + "/registration_status_table";
        }

        /**
         * Table view for Guardian Child
         * 
         * @throws Exception
         */
        @GetMapping("{clientId}/role/{roleId}/registration_status_table")
        public String child_legal_guardian_table(Model model, Locale locale,
                        @ModelAttribute("person") ChildGuardianRegistrationViewModel person,
                        @PathVariable("clientId") Long clientId,
                        @PathVariable("roleId") long roleId) {

                model.addAttribute("columns", new String[][] {
                                new String[] { messageSource.getMessage("root.add_person.firstNameLabel", null, locale),
                                                "firstName" },
                                new String[] { messageSource.getMessage("root.add_person.lastNameLabel", null, locale),
                                                "lastName" },
                                new String[] { messageSource.getMessage("root.user.add_person.birthdateLabel_noFormat",
                                                null, locale), "birthdate" },
                                new String[] { messageSource.getMessage("root.client.mitarbeiter.gallery.responseLabel",
                                                null, locale), "status" },
                                new String[] { messageSource.getMessage("root.add_person.viewGuardiansHeaderLabel",
                                                null, locale), "legalGuadians" },
                });

                model.addAttribute("guardiancolumns", new String[][] {
                                new String[] { messageSource.getMessage("root.add_person.firstNameLabel", null, locale),
                                                "firstName" },
                                new String[] { messageSource.getMessage("root.add_person.lastNameLabel", null, locale),
                                                "lastName" },
                                new String[] { messageSource.getMessage("root.add_person.emailLabel", null, locale),
                                                "email" },
                                new String[] { messageSource.getMessage("root.client.mitarbeiter.gallery.responseLabel",
                                                null, locale), "status" },
                                new String[] { messageSource.getMessage("root.add.person.clientLogin.sentPin", null,
                                                locale), "PIN" }
                });

                // get all persons that are children
                Set<Person> children = personRepository.findChildrenByClientId(clientId);

                // get logged in person
                Long person_id = personService.getPersonId();

                List<Map<String, Object>> childLegalGuardians = new ArrayList<>();

                for (Person child : children) {
                        // get the children and their associated legal guardians
                        childLegalGuardians.add(
                                        Map.of(
                                                        "id", child.getId(),
                                                        "firstName", child.getFirstName(),
                                                        "lastName", child.getLastName(),
                                                        "birthdate", child.getBirthdate(),
                                                        "status", child.getStatus(),
                                                        "legalGuardians", child.getLegalGuardians()));
                }

                model.addAttribute("childLegalGuardians", childLegalGuardians);
                model.addAttribute("id", clientId);
                model.addAttribute("title",
                                messageSource.getMessage("root.client.mitarbeiter.gallery.groupTitle", null, locale));
                model.addAttribute("children", children);
                if (model.getAttribute("guardian") == null)
                        model.addAttribute("guardian", new AdditionalGuardianViewModel());
                model.addAttribute("clientId", clientId);
                model.addAttribute("clientroleid", roleId);
                model.addAttribute("personId", person_id);

                List<PersonPermission> permissions = getPermissions(person_id);
                model.addAttribute("clientsInfo", permissions);

                PersonPermission permission = permissions.stream()
                                .filter(p -> p.getClient_role_id() == 2 || p.getClient_role_id() == 1)
                                .findFirst().orElse(null);

                if (permission != null) {
                        model.addAttribute("permission", permission);
                        return "client/clientadmin/registration_status_table";
                } else {
                        return "errors/access-denied";
                }
        }

        /**
         * Table view for the detection control
         */
        @GetMapping("{clientId}/role/{roleId}/detection_table")
        public String getDetectionTables(@PathVariable("clientId") long clientId,
                        @PathVariable("roleId") long roleId,
                        Model model,
                        Locale locale) {

                // get logged in person
                Long person_id = personService.getPersonId();

                List<PersonPermission> permissions = getPermissions(person_id);
                model.addAttribute("clientsInfo", permissions);
                PersonPermission permission = permissions.stream()
                                .filter(p -> p.getClient_role_id() == 2 || p.getClient_role_id() == 1)
                                .findFirst().orElse(null);

                model.addAttribute("permission", permission);
                String viewName = "client/mitarbeiter/detection_table";
                ViewAccess viewaccess = viewAccessService.checkPermissions(clientId, viewName);

                if (viewaccess == viewaccess.FORBIDDEN) {
                        return "errors/access-denied";
                }

                // if no person permissions found, deny access to this view
                if (permission == null) {
                        return "errors/access-denied";
                }

                model.addAttribute("columns", new String[][] {
                                new String[] { messageSource.getMessage("root.client.mitarbeiter.detectionTable.status",
                                                null, locale), "status" },
                                new String[] { messageSource.getMessage(
                                                "root.client.mitarbeiter.detectionTable.preview", null, locale),
                                                "preview" },
                                new String[] { messageSource.getMessage(
                                                "root.client.mitarbeiter.detectionTable.editLink", null, locale),
                                                "editLink" },
                                new String[] {
                                                messageSource.getMessage(
                                                                "root.client.mitarbeiter.detectionTable.imageName",
                                                                null, locale),
                                                "imageName" },
                                new String[] {
                                                messageSource.getMessage(
                                                                "root.client.mitarbeiter.detectionTable.uploadDate",
                                                                null, locale),
                                                "uploadDate" },
                                new String[] { messageSource.getMessage(
                                                "root.client.mitarbeiter.detectionTable.lastEditDate",
                                                null, locale), "lastEditDate" },
                });

                List<GalleryPicture> galleryPictures = galleryPictureRepository.findByClientId(clientId);

                // use iterator to delete elements from list galleryPictures
                for (Iterator<GalleryPicture> galleryPicture = galleryPictures.iterator(); galleryPicture.hasNext();) {
                        Set<PublicationRequest> pubRequests = publicationRequestRepository
                                        .findByClientIdAndGalleryPictureId(clientId, galleryPicture.next().getId());
                        for (PublicationRequest pubRequest : pubRequests) {
                                if (pubRequest.getStatus() == PublicationRequestStatus.REQUESTED)
                                        galleryPicture.remove();
                        }
                }

                model.addAttribute("title",
                                messageSource.getMessage("root.administration.detectionTable.title", null, locale));
                model.addAttribute("clientId", clientId);

                model.addAttribute("id", person_id);
                model.addAttribute("permission", permission);
                model.addAttribute("galleryPictures", galleryPictures);
                model.addAttribute("editLink",
                                "/client/" + Long.toString(clientId) + "/role/" + Long.toString(roleId)
                                                + "/detection_panel");

                this.addClientInfo(model, person_id);
                return "client/mitarbeiter/detection_table";
        }

        /**
         * View for the detection panel
         * 
         * @throws IOException
         *                     Table view for the identification control //Andreas:
         *                     "identification"
         */
        @GetMapping("{clientId}/role/{roleId}/detection_panel/{galleryPictureId}")
        public String getDetectionPanel(@PathVariable("clientId") long clientId,
                        @PathVariable("roleId") long roleId,
                        @PathVariable("galleryPictureId") long galleryImageId,
                        RedirectAttributes redirectAttributes,
                        Model model,
                        Locale locale) throws IOException {

                // get logged in person
                Long person_id = personService.getPersonId();

                // call person permissions of currently logged in user
                PersonPermission permission = permissionRepository
                                .findPermissionByPersonAndClientAndRole(person_id, clientId, roleId).orElse(null);

                if (permission == null) {
                        log.info("Permission is null in clientController getDetectionPanel with personId: "
                                        + Long.toString(person_id) + "clientId: " + Long.toString(clientId) + "roleId: "
                                        + Long.toString(roleId));
                        return "errors/access-denied";
                }

                GalleryPicture galleryImage = galleryPictureRepository.findById(galleryImageId).orElse(null);

                if (galleryImage == null) {
                        log.info("GalleryImage is null in clientController getDetectionPanel");
                        model.addAttribute("id", person_id);
                        this.addClientInfo(model, person_id);
                        return "client/mitarbeiter/detection_panel";
                }

                List<GalleryPictureFaceFrame> galleryPictureFaceFrameList = galleryPictureFaceFrameRepository
                                .findByGalleryPictureId(galleryImageId);

                // collect all status and coordinates for drawing
                Map<Integer[], FaceFrameGenerationStatus> drawingMap = new HashMap<>();
                List<Integer[]> coordinates = new ArrayList<>();

                for (GalleryPictureFaceFrame faceFrame : galleryPictureFaceFrameList) {
                        Integer[] coordinate = faceFrame.getCoordinates();
                        drawingMap.put(coordinate, faceFrame.getGeneration_status());
                        coordinates.add(coordinate);
                }

                String Base64EncodedgalleryImage = "";
                if (!coordinates.isEmpty()) {
                        Base64EncodedgalleryImage = PictureFunctionService.drawManyRectangles(drawingMap,
                                        galleryImage.getPictureBytes());
                } else {
                        Base64EncodedgalleryImage = galleryImage.getPictureString();
                }
                model.addAttribute("galleryImageId", galleryImageId);
                model.addAttribute("permission", permission);
                model.addAttribute("clientId", permission.getClient_id());
                model.addAttribute("roleId", permission.getClient_role_id());
                model.addAttribute("galleryImage", Base64EncodedgalleryImage);
                model.addAttribute("coordinates", new ObjectMapper().writeValueAsString(coordinates));
                model.addAttribute("detectionStatus",
                                new ArrayList<GalleryImage_DetectionStatus>(
                                                Arrays.asList(GalleryImage_DetectionStatus.values())));
                model.addAttribute("id", person_id);
                this.addClientInfo(model, person_id);
                return "client/mitarbeiter/detection_panel";
        }

        @Autowired
        IdentificationService identificationService;// Andreas 19.02.2022

        /**
         * Table view for the detection control
         */
        @GetMapping("{clientId}/role/{roleId}/identification_table")
        public String getIdentificationTables(@PathVariable("clientId") long clientId,
                        @PathVariable("roleId") long roleId,
                        Model model,
                        Locale locale) {
                // get logged in person
                Long person_id = personService.getPersonId();
                List<PersonPermission> permissions = getPermissions(person_id);
                model.addAttribute("clientsInfo", permissions);
                PersonPermission permission = permissions.stream()
                                .filter(p -> p.getClient_role_id() == 2 || p.getClient_role_id() == 1)
                                .findFirst().orElse(null);

                model.addAttribute("permission", permission);

                String viewName = "client/mitarbeiter/identification_table";
                ViewAccess viewaccess = viewAccessService.checkPermissions(clientId, viewName);

                if (viewaccess == viewaccess.FORBIDDEN) {
                        return "errors/access-denied";
                }

                // if no person permissions found, deny access to this view
                if (permission == null) {
                        return "errors/access-denied";
                }

                model.addAttribute("columns", new String[][] {
                                new String[] { messageSource.getMessage("root.client.mitarbeiter.detectionTable.status",
                                                null, locale), "status" },
                                new String[] { messageSource.getMessage(
                                                "root.client.mitarbeiter.detectionTable.preview", null, locale),
                                                "preview" },
                                new String[] { messageSource.getMessage(
                                                "root.client.mitarbeiter.detectionTable.editLink", null, locale),
                                                "editLink" },
                                new String[] {
                                                messageSource.getMessage(
                                                                "root.client.mitarbeiter.detectionTable.imageName",
                                                                null, locale),
                                                "imageName" },
                                new String[] {
                                                messageSource.getMessage(
                                                                "root.client.mitarbeiter.detectionTable.uploadDate",
                                                                null, locale),
                                                "uploadDate" },
                                new String[] { messageSource.getMessage(
                                                "root.client.mitarbeiter.detectionTable.lastEditDate", null, locale),
                                                "lastEditDate" },
                });

                List<GalleryPicture> galleryPictures = galleryPictureRepository.findByClientId(clientId);
                Map<GalleryPicture, GalleryPictureIdentificationStatus> galleryImage_status = new HashMap<>();

                for (GalleryPicture galleryPicture : galleryPictures) {
                        GalleryImage_DetectionStatus galleryImage_DetectionStatus = galleryPicture.getStatus();
                        Set<PublicationRequest> pubRequests = publicationRequestRepository
                                        .findByClientIdAndGalleryPictureId(clientId, galleryPicture.getId());
                        if (galleryImage_DetectionStatus != null
                                        && galleryImage_DetectionStatus
                                                        .equals(GalleryImage_DetectionStatus.fullyReviewed)
                                        && pubRequests.size() == 0) {
                                identificationService.checkStatus(galleryPicture);// Andreas 19.02.2022
                                GalleryPictureIdentificationStatus identificationStatus = galleryPicture
                                                .getIdentificationStatus();
                                galleryImage_status.put(galleryPicture,
                                                identificationStatus == null
                                                                ? GalleryPictureIdentificationStatus.EMPLOYEES_NOT_FINISHED
                                                                : identificationStatus);
                        }
                }
                this.addClientInfo(model, person_id);
                model.addAttribute("title",
                                messageSource.getMessage("root.administration.identificationTable.title", null,
                                                locale));
                model.addAttribute("clientId", clientId);
                model.addAttribute("permission", permission);
                model.addAttribute("galleryPictures", galleryImage_status);
                model.addAttribute("id", clientId);
                model.addAttribute("editLink", "/identificationPanel/" + clientId + "/" + roleId + "/");

                return "client/mitarbeiter/identification_table";
        }
}
