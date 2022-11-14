package com.klix.backend.controller.user;

import java.util.Locale;
import java.util.NoSuchElementException;

import com.klix.backend.controller.BaseImageController;
import com.klix.backend.exceptions.FaceException;
import com.klix.backend.exceptions.StorageException;
import com.klix.backend.model.IdPicture;
import com.klix.backend.model.Person;
import com.klix.backend.model.User;
import com.klix.backend.service.picture.PictureUploadService;

import org.springframework.ui.Model;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/user/profile/img/**")
public class ProfileImageController extends BaseImageController {

    @Autowired private PictureUploadService pictureUploadService;

    /**
     * Get: Load the current list of uploaded files to Thymeleaf template.
     * 
     * @return Die Thymeleaf /user/uploadForm Seite
     */
    @GetMapping("/upload")//Ergänzung personId: Andreas
    public String showPersonFileUpload(@RequestParam(required = false) Long personId, Model model, Locale locale) {
    	
        try {
        	if (personId==null) personId=findLoggedUser(locale).getPerson().getId();
        	else findLoggedUser(locale);// Andreas: Die Abfrage belasse ich aus Sicherheitsgründen 
        } catch (NoSuchElementException e) {
            log.error("User not found in showPersonFileUpload"+e.getMessage());
            return "user/profile/img/noupload";
        }
        if (checkPersonId(personId)) {
            IdPicture loadedImage = idPictureRepository.findByPersonId(personId).orElse(null);
           
            this.addClientInfo(model, personId);
            model.addAttribute("id", personId);
            model.addAttribute("images", loadedImage);
          
            return "user/profile/img/upload";
        } else {
            log.error("IdPicture not found in showUserFileUpload");
            return "user/profile/img/noupload";
        }
    }


    /**
     * Post: Upload the file and store to the picture persistence service
     * 
     * @return Die Thymeleaf /user/upload Seite (per redirect)
     * @throws Exception
     */
    @PostMapping("/upload")
    public String handleFileUpload(@RequestParam(required = false) Long personId,//Andreas 21.03.22
    		@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes, Locale locale)
        throws Exception {
        
        //first check that file fullfills all technical requirements
        boolean allowedFile = checkMultipartFile(file);
        boolean allowedImageType = checkImageType(file);
        
        String redirectString="redirect:/user/profile/img/upload"+(personId == null ? "" : "?personId="+personId);

        if (allowedFile && allowedImageType ) {
            Person person;
            
            try 
            {
                User user = findLoggedUser(locale);
                person = user.getPerson();
            }catch (NoSuchElementException e) {
                redirectAttributes.addFlashAttribute("message_error", e.getMessage());
                return redirectString;//?? Wenn kein User oder keine Person gefunden wird, soll die Person den upload noch einmal probieren? (fragt Andreas am 21.03.22)
            }
            if (personId!=null) {
            	redirectAttributes.addAttribute("personId", personId);
            	person=personRepository.findById(personId).orElse(null);
            }
            try
            {
                //crop, resize and compress and store the idpicture
                pictureUploadService.storeIdPicture(file, person);      
                // Set a confirmation message if the file was uploaded successfully
                String message = messageSource.getMessage("root.user.userFileUploadController.uploadSuccess", null, locale);
                redirectAttributes.addFlashAttribute("message_success", String.format(message, file.getOriginalFilename() + "!"));
            
            //catch eror if semantics of the image is not correct
            }catch (FaceException e)
            {
                log.info("catching Face Exception in profileimage file "+e.getMessage());
                String message="";

                // if the AI found no face.
                if(e.getMessage().equals("InvalidBase64Exception")){
                    message = messageSource.getMessage("root.user.userFileUploadController.technicalProblem", null, locale);
                }

                // if the AI found no face.
                if(e.getMessage().equals("NoFaceException")){
                    message = messageSource.getMessage("root.user.userFileUploadController.noFace", null, locale);
                }

                //if python backend cant work with file
                if(e.getMessage().equals("FormatException")){
                    message = messageSource.getMessage("root.user.userFileUploadController.uploadError_fileType", null, locale);
                }

                // if the AI found more than one face.
                else if(e.getMessage().equals("2face")){ 
                    message = messageSource.getMessage("root.user.userFileUploadController.faceError", null, locale);
                }

                // if not exactly four coordinates were returned.
                else if(e.getMessage().equals("4cords")){
                    message = messageSource.getMessage("root.user.userFileUploadController.notFourCoordinates", null, locale);
                }

                // if not connected to Docker port properly.
                else if(e.getMessage().equals("ConnectionException")){
                    message = messageSource.getMessage("root.user.userFileUploadController.technicalProblem", null, locale);
                }

                redirectAttributes.addFlashAttribute("message_error", String.format(message, file.getOriginalFilename() + "!"));
            }

            return redirectString;
        }
        else {
            log.info("Error occured while uploading the image at profileImageController.");
            String message = "";
            if(!allowedImageType) message = messageSource.getMessage("root.user.userFileUploadController.uploadError_fileType", null, locale);
            if(!allowedFile) message = messageSource.getMessage("root.user.userFileUploadController.uploadError", null, locale);
            redirectAttributes.addFlashAttribute("message_error", message);
            return redirectString;
        }
    }


    /**
     * RequestMapping: Delete the persisted image
     * 
     * @return Die Thymeleaf /user/upload Seite
     */
    @RequestMapping("/delete/{id}/{personId}")
    public String deleteFile(@PathVariable("id") long id, @PathVariable("personId") long personId,
    		RedirectAttributes redirectAttributes, Model model, Locale locale)
            throws StorageException {
        if (checkIdPictureId(id)) 
        {
            picturePersistenceService.delete(id, idPictureRepository);

            // Get a confirmation message if the file was uploaded successfully
            String message = messageSource.getMessage("root.user.userFileUploadController.deleteSuccess", null, locale);

            redirectAttributes.addFlashAttribute("message_success", String.format(message, id + "!"));
        } else {
            // Get an error message if the file was not uploaded successfully
            String message = messageSource.getMessage("root.user.userFileUploadController.deleteError", null, locale);
            redirectAttributes.addFlashAttribute("message_error", message);
        }

        return "redirect:/user/profile/img/upload?personId="+personId;
    }

    @ExceptionHandler(StorageException.class)
    public ResponseEntity<?> handleStorageFileNotFound(StorageException exc) {
        return ResponseEntity.notFound().build();
    }

    
}
