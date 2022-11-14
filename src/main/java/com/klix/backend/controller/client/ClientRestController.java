package com.klix.backend.controller.client;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.klix.backend.controller.BaseImageController;
import com.klix.backend.enums.FaceFrameGenerationStatus;
import com.klix.backend.enums.GalleryImage_DetectionStatus;
import com.klix.backend.model.GalleryPicture;
import com.klix.backend.model.GalleryPictureFaceFrame;
import com.klix.backend.service.picture.FaceFrameService;
import com.klix.backend.service.picture.PictureFunctionService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/client/*")
public class ClientRestController extends BaseImageController
{
    @Autowired private FaceFrameService faceFrameService;


    /**
     * Post: Diese Methode löscht ausgewählte Koordinates einen GalleryImages
     * @param id Die id des galleryImages
     * @param ids Die Liste mit den zum Löschen ausgewahlten Id's
     */
    @PostMapping("/coordinate_delete/{id}")
    public @ResponseBody ResponseEntity<String> delete_coordinates(@PathVariable("id") long galleryImageId, @RequestBody Integer[] coordinates, Locale locale) 
    {
        if(coordinates.length == 4) {
            picturePersistenceService.deleteCoordinates(galleryImageId, coordinates);
            // need to return something on POST
            return new ResponseEntity<>("{}", HttpStatus.OK);
        } else {
            return new ResponseEntity<>("{}", HttpStatus.BAD_REQUEST);
        }
        
    }

    /**
     * Post: Diese Methode speichert eingezeichnete Koordinates auf einem GalleryImages
     * @param id Die id des galleryImages
     * @param coordinates Die Liste mit dem vom user markierten Koordinates
     * @throws IOException
     */
    @PostMapping("/coordinate_save/{id}")
    public @ResponseBody ResponseEntity<String> save_coordinates(@PathVariable("id") long galleryImageId, @RequestBody Integer[] coordinates, Locale locale) throws IOException 
    {
        if (coordinates.length != 4) {
            return new ResponseEntity<>("{}", HttpStatus.BAD_REQUEST);
        }

        faceFrameService.createFaceFrameGeneratedByHuman(galleryImageId, coordinates);

        // need to return something on POST
        return new ResponseEntity<>("{}", HttpStatus.OK);
    }

    /**
     * Post: delivers back newly drawn image with refreshed coordinates
     * @param id Die id des galleryImages
     * @return 
     * @throws IOException
     */
    @GetMapping("/getRefreshedFrames/{id}")
    public @ResponseBody ResponseEntity<String> get_refreshedFrame(@PathVariable("id") long galleryImageId, Locale locale) throws IOException 
    {
        GalleryPicture galleryImage= galleryPictureRepository.findById(galleryImageId).orElse(null);
       
        if (galleryImage == null) {
            return new ResponseEntity<>("", HttpStatus.BAD_REQUEST);
        }

        List<GalleryPictureFaceFrame> galleryPictureFaceFrameList = galleryPictureFaceFrameRepository.findByGalleryPictureId(galleryImageId);

        //collect all status and coordinates for drawing
        Map<Integer[], FaceFrameGenerationStatus> drawingMap = new HashMap<>();

        for (GalleryPictureFaceFrame faceFrame : galleryPictureFaceFrameList) {
            Integer[] coordinate = faceFrame.getCoordinates();
            drawingMap.put(coordinate, faceFrame.getGeneration_status());
        }

        String Base64EncodedgalleryImage = PictureFunctionService.drawManyRectangles(drawingMap, galleryImage.getPictureBytes());

        return new ResponseEntity<>(new ObjectMapper().writeValueAsString(Base64EncodedgalleryImage), HttpStatus.OK);
    }

    /**
     * Post: Diese Methode speichert neu gesetzten Status eines Gallerybildes
     * @param id Die id des galleryImages
     * @param coordinates Die Liste mit dem vom user markierten Koordinates
     */
    @PostMapping("/set_detectionStatus/{id}/{status_id}")
    public @ResponseBody ResponseEntity<String> set_detectionStatus(@PathVariable("id") long galleryImageId, 
                                                                    @PathVariable("status_id") int statusId, Locale locale) 
    {
        GalleryImage_DetectionStatus status = GalleryImage_DetectionStatus.values()[statusId];
        picturePersistenceService.saveGalleryImageStatus(galleryImageId, status);

        // need to return something on POST
        return new ResponseEntity<>("{}", HttpStatus.OK);
    }


     /**
     * Post: delivers back newly drawn image with refreshed coordinates
     * @param id Die id des galleryImages
     * @return 
     * @throws IOException
     */
    @GetMapping("/getFrameCoordinates/{id}")
    public @ResponseBody ResponseEntity<String> get_frameCoordinates(@PathVariable("id") long galleryImageId, Locale locale) throws IOException 
    {
        List<Integer[]> coordinates = picturePersistenceService.getFaceFrameCoordinates(galleryImageId);
        log.info(Integer.toString(coordinates.size()));
        if(coordinates.isEmpty()) {
            return new ResponseEntity<>("", HttpStatus.BAD_REQUEST);
        } else {
            return new ResponseEntity<>(new ObjectMapper().writeValueAsString(coordinates), HttpStatus.OK);
        }
    }
}   