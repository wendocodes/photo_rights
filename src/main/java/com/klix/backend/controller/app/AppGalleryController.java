package com.klix.backend.controller.app;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.klix.backend.enums.PublicationResponseStatus;
import com.klix.backend.model.GalleryPicture;
import com.klix.backend.model.Person;
import com.klix.backend.model.PublicationRequest;
import com.klix.backend.model.PublicationResponse;
import com.klix.backend.model.User;
import com.klix.backend.repository.GalleryPictureRepository;
import com.klix.backend.repository.PublicationRequestRepository;
import com.klix.backend.repository.PublicationResponseRepository;
import com.klix.backend.service.app.JwtUtilsService;
import com.klix.backend.service.picture.PictureUploadService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;


/**
 * TODO: Kommentar
 */
@Slf4j
@RestController
@RequestMapping(path = "/app/api/v01/gallery/**")
@CrossOrigin(origins = "*")
public class AppGalleryController
{
    @Autowired private PictureUploadService pictureUploadService;

    @Autowired private JwtUtilsService jwtUtilsService;

    @Autowired private PublicationResponseRepository publicationResponseRepository;
    @Autowired private PublicationRequestRepository publicationRequestRepository;
    @Autowired private GalleryPictureRepository galleryPictureRepository;


    /**
     * accepts form data containing images. If valid JWT-Token, add the images to the gallery of the person associated to user.
     */
    @PostMapping("/upload")
    public ResponseEntity<String> uploadPictures(final HttpServletRequest request, final HttpServletResponse response, @RequestParam("files") MultipartFile[] files)
        throws IOException
    {
        // assert file existence
        if (files.length == 0)
        {
            log.debug("no files found in uploadPictures. Exit.");
            return new ResponseEntity<>("No files found", HttpStatus.BAD_REQUEST);
        }

        // !TODO error handling
        User user = jwtUtilsService.getUser(request, response);
        if (user == null)
        {
            log.warn("in uploadPictures(): User not found. Exit.");
            return new ResponseEntity<>("User not found", HttpStatus.BAD_REQUEST);
        }

        // get person of current user
        Person person = user.getPerson();
        if (person == null)
        {
            log.warn("in uploadPictures(): User with name " + user.getUsername() + " has no related person instance. Exit.");
            return new ResponseEntity<>("User has no related person instance", HttpStatus.BAD_REQUEST);
        }
        log.debug("in uploadPictures(): Got person " + person.toString());

        // save all images
        List<String> errorOnSave = new ArrayList<>();
         for (MultipartFile file : files)
         {
            try {
                pictureUploadService.storeGalleryPicture(file, user.getPerson());
            } catch (Exception e) {
                log.error(e.getMessage());
                errorOnSave.add(file.getOriginalFilename());
            }
        }

        // inform about upload
        if (!errorOnSave.isEmpty())
        {
            return new ResponseEntity<>(String.join(" , ", errorOnSave), HttpStatus.PAYLOAD_TOO_LARGE);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }


    /**
     * Lets a user with a valid JWT-token retrieve all his/her existing publication requests.
     * (All requests to publish an image with him/her on it)
     */
    @GetMapping("/pubRequest")
    public List<Map<String, Object>> getPublicationRequests(final HttpServletRequest request, final HttpServletResponse response)
        throws IOException
    {
        User user = jwtUtilsService.getUser(request, response);
        if (user == null)
        {
            log.warn("in getPublicationRequests(): user not found, " + response.toString());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return new ArrayList<>();
        }
        
        Person person = user.getPerson();
        if (person == null)
        {
            log.warn("in getPublicationRequests(): user has no associated person");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return new ArrayList<>();
        }


        // look through all PublicationResponses of current user (or actually its person)
        Set<PublicationResponse> responses = this.publicationResponseRepository.findByPersonId(person.getId());
        log.info("Responses : "+ responses);
        List<Map<String, Object>> entries = new ArrayList<>();  // is going to be returned
        for (PublicationResponse res : responses)
        {
            // get some instances that shouldn't be null
            PublicationRequest req = this.publicationRequestRepository.findById(res.getPublicationRequestId()).orElse(null);
            log.info("Requests : {} Response : {}", req, res );
            if (req == null)
            {
                // !TODO better error handling
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return new ArrayList<>();
            }

            GalleryPicture pic = this.galleryPictureRepository.findById(req.getGalleryPictureId()).orElse(null);
            log.info("pics : {} Response : {}", pic, res );  
            if (pic == null)
            {
                // !TODO better error handling
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return new ArrayList<>();
            }

            // assemble for sending back
            Map<String, Object> entity = new HashMap<>();
            entity.put("id", res.getId());
            entity.put("text", req.getText());
            entity.put("image", "data:" + pic.getType() +";base64, " + pic.getPictureString());
            entity.put("date", req.getCreatedAt());
            entity.put("status", res.getStatus().ordinal());

            entries.add(entity);
        }

        // send retrieved
        response.setStatus(HttpServletResponse.SC_OK);
        return entries;
    }


    /**
     * Update the status of the publication request with given id, if valid JWT-token and user is on the picture.
     */
    @PostMapping("/pubRequest/{id}")
    public void postPublicationRequest(@PathVariable("id") long id, @RequestBody PublicationResponseStatus status, final HttpServletRequest request, final HttpServletResponse response)
        throws IOException
    {
        // debug
        log.debug("Publication request id: " + Long.toString(id) + ", new status: " + status.name());

        // get all the necessary values, ideally not null
        User user = jwtUtilsService.getUser(request, response);
        if (user == null)
        {
            log.warn(response.toString());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        Person person = user.getPerson();
        if (person == null)
        {
            log.warn("in postPublicationRequests(): user has no associated person");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        PublicationResponse res = this.publicationResponseRepository.findById(id).orElse(null);
        if (res == null || !res.getPersonId().equals(person.getId()))
        {
            log.warn("in postPublicationRequests(): PublicationResponse not found");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        // set new status
        res.setStatus(status);
        res.setLastChanged(new Date());
        this.publicationResponseRepository.save(res);
        
        response.setStatus(HttpServletResponse.SC_OK);
    }
}
