package com.klix.backend.service.picture;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.util.Base64;

import javax.imageio.ImageIO;

import com.google.gson.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.klix.backend.model.api.cascadedetector.Request;
import com.klix.backend.service.DockerContainerService;
import com.klix.backend.service.JsonRequestService;
import com.klix.backend.enums.AiContainer;
import com.klix.backend.exceptions.FaceException;
import com.klix.backend.exceptions.StorageException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PictureCropService
{
    @Autowired private JsonRequestService jsonRequestService;
    @Autowired private DockerContainerService dockerContainerService;
    

    /**
     * detects faces.
     * 
     * @return coordinates
     */
    public Integer[][] computeFaceCoordinates(byte[] imageBitmap)
        throws StorageException, JsonProcessingException, FaceException, ConnectException {

        Request request = new Request(Integer.toString(0), Base64.getEncoder().encodeToString(imageBitmap));
        String wcResponse = null;

        if(!dockerContainerService.isContainerUp(AiContainer.CNN))
        {
            throw new FaceException("ConnectionException");
        }

        //no errorHandling here, just propagate back to controller
        wcResponse = jsonRequestService.sendJsonRequest(AiContainer.CNN.getUrl() + "upload", request, String.class);

        // process response & get face coordinates
        log.debug("Response of " + AiContainer.CNN.getUrl() + "upload: " + wcResponse);

        JsonObject responseBody = JsonParser.parseString(wcResponse).getAsJsonObject();

        if (!responseBody.get("coordinates").isJsonArray())
        {
            throw new StorageException("coordinates is not an array");
        }
              
        JsonArray faces = responseBody.get("coordinates").getAsJsonArray();
        
        Integer[][] faceCoordinates = new Integer[faces.size()][4];
        for (int i = 0; i < faces.size(); i++) 
        {
            JsonArray face = faces.get(i).getAsJsonArray();
            faceCoordinates[i][0] = face.get(0).getAsInt();
            faceCoordinates[i][1] = face.get(1).getAsInt();
            faceCoordinates[i][2] = face.get(2).getAsInt();
            faceCoordinates[i][3] = face.get(3).getAsInt();
        }
        
        // crop IdPicture to face coordinates, containing only detected face
        return faceCoordinates;
    }


    /**
     * crops the image 
     * @param image
     * @param x
     * @param y
     * @param w
     * @param h
     * @param type
     * @return cropped image bytes
     */
    public byte[] cropImage(byte[] image, Integer[] coordinates) {
            int x = coordinates[0];
            int y = coordinates[1];
            int w = coordinates[2];
            int h = coordinates[3];

            try {
                ByteArrayInputStream bis = new ByteArrayInputStream(image);
                BufferedImage originalImage = ImageIO.read(bis);

                // crop IdPicture to face coordinates, containing only detected face
                BufferedImage croppedImage = originalImage.getSubimage(x, y, w, h);

                ByteArrayOutputStream output = new ByteArrayOutputStream();
                ImageIO.write(croppedImage, "jpg", output);
                return output.toByteArray();

            } catch (IOException e) {
                log.error("Exception Occured :{}", e.getMessage());
                return new byte[]{};
            } 
    }
}
