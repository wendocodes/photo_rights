package com.klix.backend.controller.app;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.klix.backend.exceptions.FaceException;
import com.klix.backend.exceptions.StorageException;
import com.klix.backend.exceptions.TypeException;
import com.klix.backend.model.IdPicture;
import com.klix.backend.model.Person;
import com.klix.backend.model.User;
import com.klix.backend.service.app.JwtUtilsService;
import com.klix.backend.service.picture.PictureUploadService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;


@Slf4j
@RestController
@RequestMapping(path = "/app/api/v01/profile/**")
@CrossOrigin(origins = "*")
public class AppProfileController
{
    @Autowired private PictureUploadService pictureUploadService;

    @Autowired private JwtUtilsService jwtUtilsService;

    @Autowired private MessageSource messageSource;


    /**
     * TODO: Kommentar
     */
    @PostMapping("/idPictureUpload")
    public ResponseEntity<Object> uploadIdPicture(final HttpServletRequest request, final HttpServletResponse response,
            @RequestParam("file") MultipartFile file) throws IOException {
        // assert file existence
        if (file == null || file.isEmpty())
        {
            log.debug("no files found in uploadIdPicture. Exit.");
            return new ResponseEntity<>("No file found", HttpStatus.BAD_REQUEST);
        }

        // get current user
        User user = jwtUtilsService.getUser(request, response);
        if (user == null) {

            // not authenticated or not known at all
            log.warn("in uploadIdPicture(): User attempted to upload gallery images but could not be found. Exit.");
            return new ResponseEntity<>("User could not be found", HttpStatus.BAD_REQUEST);
        }

        // get person of current user
        Person person = user.getPerson();
        if (person == null) {
            log.warn("in uploadIdPicture(): User with name " + user.getUsername()
                    + " has no related person instance. Exit.");
            return new ResponseEntity<>("User has no related person instance", HttpStatus.BAD_REQUEST);
        }
        log.debug("in uploadIdPicture(): Got person " + person.toString());

        /**
         *  check if the photo has been cropped, save the croppedPicture
         *  @returns either the cropped photo or a bad request error if image is too large
         */
        try {
            log.info("Saving and Cropping Image");
            IdPicture picture = pictureUploadService.storeIdPicture(file, person);
            log.info("Cropped Image : {}", picture != null);

            if (picture == null)
            {
                return new ResponseEntity<>(file.getOriginalFilename(), HttpStatus.BAD_REQUEST);
            }

            // everything worked, return image data with status of 200
            Map<String, Object> imageResponse = new HashMap<>();
            imageResponse.put("base64Image", "data:" + picture.getType() + ";base64," + picture.getPictureString());

            return new ResponseEntity<>(imageResponse, HttpStatus.OK);

        } catch (StorageException e) {
            log.error(e.getMessage());
            String message = messageSource.getMessage("root.user.userFileUploadController.uploadError", null, null);

            return new ResponseEntity<>(message, HttpStatus.PAYLOAD_TOO_LARGE);
        } catch (IOException e) {
            log.error(e.getMessage());
            return new ResponseEntity<>(file.getOriginalFilename(), HttpStatus.BAD_REQUEST);
        } catch(FaceException e){
            log.info("in face exception app",e.toString()," another",e.getMessage()," another 2 ",e.getCause()," three",e);
            //String message = messageSource.getMessage("root.user.userFileUploadController.faceError", null, null);
            
            //build response object to send to app
            Map<String, String> obj = new HashMap<>();
            obj.put("filename", file.getOriginalFilename());
            obj.put("message",e.toString());
            return new ResponseEntity<>(obj,HttpStatus.BAD_REQUEST);
            
        }catch(TypeException e){
            //String message = messageSource.getMessage("root.user.userFileUploadController.uploadError_fileType", null, null);
            
            //build response object to send to app
            Map<String, String> obj = new HashMap<>();
            obj.put("filename", file.getOriginalFilename());
            obj.put("message",e.toString());

            return new ResponseEntity<>(obj,HttpStatus.BAD_REQUEST);
        }
    }


    /**
     * TODO: Kommentar
     */
    @GetMapping("/person")
    public Map<String, Object> getPerson(final HttpServletRequest request, final HttpServletResponse response)
            throws IOException {

        // get current user
        User user = jwtUtilsService.getUser(request, response);
        if (user == null)
        {
            // not authenticated or not known at all
            log.warn("in getPerson(): User attempted to upload gallery images but could not be found. Exit.");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return new HashMap<>();
        }

        // get person of current user
        Person person = user.getPerson();
        if (person == null) {
            log.warn("in getPerson(): User with name " + user.getUsername() + " has no related person instance. Exit.");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return new HashMap<>();
        }
        log.debug("in getPerson(): Got person " + person.toString());

        Map<String, Object> personRes = new HashMap<>();
        personRes.put("firstName", person.getFirstName());
        personRes.put("lastName", person.getLastName());
        personRes.put("email", person.getEmail());
        personRes.put("birthdate", person.getBirthdate());
        personRes.put("id", person.getId());
        
        Map<String,Object> finalRes =new HashMap<>();
        finalRes.put("person", personRes);

       // Use an ArrayList to send associated persons alongside logged in user
        Set<Person> personList = person.getLegalGuardians();
        // Map<String,Object> childArr=new HashMap<>();
        List<Map<String, Object>> data = new ArrayList<>();
        //var i=0;
        for(Person child : personList){
            log.info("childFirstName "+child.getFirstName());
            Map<String,Object> childArr=new HashMap<>();
            childArr.put("childFirstName", child.getFirstName());
        childArr.put("childLastName", child.getLastName());
        childArr.put("childEmail", child.getEmail());
            childArr.put("childBirthdate",child.getBirthdate());
        data.add(childArr);    
        
        }

        finalRes.put("childdata",data);
        return finalRes;
    }
}
