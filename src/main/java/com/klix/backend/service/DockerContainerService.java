package com.klix.backend.service;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.ConnectException;

import com.klix.backend.model.api.cascadedetector.Request;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.klix.backend.enums.AiContainer;
import com.klix.backend.exceptions.FaceException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 * 
 */
@Service
@Slf4j
public class DockerContainerService
{
    @Autowired private JsonRequestService jsonRequestService;

    /**
     * we use this function to disable/able button on devpage for uploading to the specific ai
     * @throws IOException
     */
    public boolean isContainerUp(AiContainer container)
    {

        // get url of container
        String baseUrl = container.getUrl();
        String urlExp = baseUrl != null ? baseUrl : "null";
        log.debug("URL of " + container.name() + " is " + urlExp);

        if (baseUrl == null)
        {
            log.warn("Could not find base url of container. Exit.");
            return false;
        }
        
        try{
            // send request to container
            // if the ai container answers (with not null), it is alive!!!
            return jsonRequestService.sendJsonRequest(baseUrl + "upload", new Request("0", ""), String.class) != null;
        
        } catch (JsonProcessingException e) {
            log.info("recieved JsonProcessingException from" + container.name() + "with error message"+ e.getMessage());
            return false;
        } catch (FaceException e) {
            // return true, because container exists and has answered, no matter with what.
            return true;
        } catch (ConnectException e) {
            return false;
         }
    }
}
