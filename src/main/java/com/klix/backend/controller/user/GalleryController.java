package com.klix.backend.controller.user;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.klix.backend.model.GalleryPicture;
import com.klix.backend.model.Person;
import com.klix.backend.model.PublicationRequest;
import com.klix.backend.model.PublicationResponse;
import com.klix.backend.model.User;
import com.klix.backend.controller.BaseImageController;
import com.klix.backend.enums.PublicationResponseStatus;
import com.klix.backend.exceptions.StorageException;
import com.klix.backend.repository.projections.PersonPermission;
import com.klix.backend.service.picture.PictureUploadService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/user/**")
public class GalleryController extends BaseImageController {
    /**
     * Get: Upload
     * 
     * @return Die Thymeleaf "develop/files/uploadForm" Seite
     */
    @GetMapping("/gallery")
    public String showFileUpload(Model model, Locale locale) {
        // only get own pics
        User user = null;
        try {
            user = findLoggedUser(locale);
            Person person = user.getPerson();
            List<PersonPermission> permissions = getPermissions(person.getId());
            model.addAttribute("clientsInfo", permissions);
            model.addAttribute("id", person.getId());
            model.addAttribute("pname", person.getFirstName() + " " + person.getLastName());
        } catch (NoSuchElementException e) {
            log.error("NoSuchElementException after showFileUpload in GalleryController: " + e.getMessage());
            return "user/gallery";
        }

        model.addAttribute("images", galleryPictureRepository.findByUploader(user.getPerson()));

        return "user/gallery";
    }

    /**
     * Post:
     * 
     * @return Die Thymeleaf "client/uploadForm" Seite per Redirect
     */
    @PostMapping("/gallery")
    public String handleFileUpload(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes,
            Locale locale)
            throws IOException, InterruptedException {

        String errorMessageKey = "message_error";
        // first check that file fullfills all technical requirements
        boolean allowedFile = checkMultipartFile(file);
        boolean allowedImageType = checkImageType(file);

        if (allowedFile && allowedImageType) {
            User user;
            try {
                user = findLoggedUser(locale);
            } catch (NoSuchElementException e) {
                redirectAttributes.addFlashAttribute(errorMessageKey, e.getMessage());
                return "redirect:/user/gallery";
            }
            try {
                GalleryPicture pic = pictureUploadService.storeGalleryPicture(file, user.getPerson());
                if (pic == null) {
                    String messageError = messageSource.getMessage("root.dev.developController.uploadError", null,
                            locale);
                    redirectAttributes.addFlashAttribute(errorMessageKey, messageError);
                    return "redirect:/user/gallery";
                }

            } catch (StorageException e) {
                log.error(e.toString());

                String message = messageSource.getMessage("root.user.userFileUploadController.uploadError", null,
                        locale);
                redirectAttributes.addFlashAttribute(errorMessageKey, message);
                return "redirect:/user/gallery";

            }

            String message = messageSource.getMessage("root.dev.developController.uploadSuccess", null, locale);
            redirectAttributes.addFlashAttribute("message_success", String.format(message, file.getOriginalFilename()));
            return "redirect:/user/gallery";

        } else {
            log.info("Error occured while uploading the image at galleryController.");
            String message = "";
            if (!allowedImageType)
                message = messageSource.getMessage("root.user.userFileUploadController.uploadError_fileType", null,
                        locale);
            if (!allowedFile)
                message = messageSource.getMessage("root.user.userFileUploadController.uploadError", null, locale);

            redirectAttributes.addFlashAttribute("message_error", message);
            return "redirect:/user/gallery";
        }

    }

    /**
     * RequestMapping: Delete the persisted image
     * 
     * @return Die Thymeleaf /user/upload Seite
     */
    @RequestMapping("/gallery/deleteImg/{id}")
    public String deleteImg(@PathVariable("id") long id, RedirectAttributes redirectAttributes, Model model,
            Locale locale) {
        if (checkGalleryPictureId(id)) {
            log.debug("delete GalleryPicture with id: " + Long.toString(id));
            deleteRequests(id);
            picturePersistenceService.delete(id, galleryPictureRepository);

            // Get a confirmation message if the file was uploaded successfully
            String message = messageSource.getMessage("root.user.userFileUploadController.deleteSuccess", null, locale);

            redirectAttributes.addFlashAttribute("message", String.format(message, id + "!"));
        } else {
            // Get an error message if the file was not uploaded successfully
            String message = messageSource.getMessage("root.user.userFileUploadController.deleteError", null, locale);
            redirectAttributes.addFlashAttribute("message_error", message);
        }

        return "redirect:/user/gallery";
    }

    public void deleteRequests(long galleryPictureId) {

        Set<PublicationRequest> requests = this.publicationRequestRepository.findByGalleryPictureId(galleryPictureId);
        log.info("Requests: " + requests);
        if (!requests.isEmpty()) {
            for (PublicationRequest request : requests) {
                Set<PublicationResponse> responses = this.publicationResponseRepository
                        .findByPublicationRequestId(request.getId());
                this.publicationResponseRepository.deleteAll(responses);
                this.publicationRequestRepository.deleteById(request.getId());
            }
        }
    }

    /**
     * Lets a user with a valid JWT-token retrieve all his/her existing publication
     * requests.
     * (All requests to publish an image with him/her on it)
     * 
     * @throws IOException
     */
    @GetMapping("/pubRequest")
    public String getPublicationRequests(Model model, Locale locale) throws IOException {
        Person person;
        try {
            User user = findLoggedUser(locale);
            person = user.getPerson();

            List<PersonPermission> permissions = getPermissions(person.getId());
            model.addAttribute("clientsInfo", permissions);
            model.addAttribute("id", person.getId());
            model.addAttribute("pname", person.getFirstName() + " " + person.getLastName());
        } catch (NoSuchElementException e) {
            model.addAttribute("error", e.getMessage());
            return "user/publicationRequests";
        }

        // look through all PublicationResponses of current user (or actually its
        // person), ab 24.05.22: auch der Kinder
        Set<PublicationResponse> responses = this.publicationResponseRepository
                .findByPersonIdOrderByPublicationRequestIdDesc(person.getId());
        List<Long> childIDs = personRepository.findChildrensIDsOfPerson(person.getId());// Weekly 24.05.22, Johannes:
                                                                                        // Langfristig ein Account trotz
                                                                                        // unterschiedlicher
                                                                                        // Institutionen / Kunden.
                                                                                        // Daraus folgt: nicht filtern?
        for (long childID : childIDs) {
            responses.addAll(publicationResponseRepository.findByPersonIdOrderByPublicationRequestIdDesc(childID));
        }
        // List<Map<String, Object>> entries = new ArrayList<>(); // is going to be
        // returned
        TreeMap<Long, Map<String, Object>> entriesMap = new TreeMap<>();
        Set<Long> personIds = new TreeSet<Long>();
        personIds.addAll(childIDs);
        personIds.add(person.getId());
        for (PublicationResponse res : responses) {
            // get some instances that shouldn't be null
            PublicationRequest req = this.publicationRequestRepository.findById(res.getPublicationRequestId())
                    .orElse(null);
            if (req == null) {
                model.addAttribute("error", "PublicationResponse has reference to non-existing PublicationRequest");
                return "user/publicationRequests";
            }

            GalleryPicture pic = this.galleryPictureRepository.findById(req.getGalleryPictureId()).orElse(null);
            if (pic == null) {
                model.addAttribute("error", "PublicationRequest has reference to non-existing GalleryPicture");
                return "user/publicationRequests";
            }
            Map<String, Object> entry = entriesMap.get(pic.getId());
            if (entry == null) {
                // make shallow copy of object galleryImage to make sure everyone understand
                // that we work on blurred Image
                GalleryPicture blurry_pic = pic;
                // blurry_pic.setPictureBytes(pictureBlurredService.drawBluredFacesExceptReceiver(person.getId(),
                // blurry_pic));
                blurry_pic.setPictureBytes(pictureFunctionService.drawBluredFacesExceptPersons(personIds, blurry_pic));
                String statusString;
                PublicationResponseStatus status = res.getStatus(person.getId());
                if (status == null) {
                    log.error(
                            "Es scheint ein alte inkompatible  Bildanfrage vorzuliegen. Der Person kann kein Responsestatus zugeordnet werden. Hilfsweise wird der Gesamtstatus gewählt.");
                    status = res.getStatus();
                }
                switch (status) {
                    case PENDING:
                        statusString = messageSource.getMessage("root.user.index.publicationRequestStatus.pending",
                                null, locale);
                        break;
                    case ALLOWED:
                        statusString = messageSource.getMessage("root.user.index.publicationRequestStatus.allowed",
                                null, locale);
                        break;
                    case FORBIDDEN:
                        statusString = messageSource.getMessage("root.user.index.publicationRequestStatus.forbidden",
                                null, locale);
                        break;
                    default:
                        statusString = "?";
                }
                // assemble for sending back
                Map<String, Object> m = new HashMap<>();
                m.put("request", req);
                m.put("status", status);
                m.put("responseIds", new StringBuffer("" + res.getId()));
                m.put("image", blurry_pic);
                m.put("statusString", statusString);
                entriesMap.put(pic.getId(), m);
            } else {
                ((StringBuffer) entry.get("responseIds")).append("," + res.getId());
            }
        }
        model.addAttribute("requests", entriesMap.values());
        model.addAttribute("PublicationResponseStatus", PublicationResponseStatus.class);
        return "user/publicationRequests";
    }

    /**
     */
    @PostMapping("/pubRequest/{ids}")
    public String postPublicationRequest(@PathVariable("ids") List<Long> ids,
            @ModelAttribute("status") String statusName, RedirectAttributes redirectAttributes, Locale locale)
            throws IOException {
        PublicationResponseStatus status = PublicationResponseStatus.valueOf(statusName);
        // debug
        log.debug("Publication response ids: " + ids + ", new status: " + status.name());
        Person loggedInPerson;
        try {
            User user = findLoggedUser(locale);
            loggedInPerson = user.getPerson();
        } catch (NoSuchElementException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/user/pubRequest";
        }
        List<PublicationResponse> responses = this.publicationResponseRepository.findAllById(ids);
        for (PublicationResponse res : responses) {
            res.putStatus(loggedInPerson.getId(), status);
            res.setLastChanged(new Date());
            this.publicationResponseRepository.save(res);
        }
        return "redirect:/user/pubRequest";
    }
}
