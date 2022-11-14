package com.klix.backend.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.klix.backend.model.api.cascadedetector.Request;
import com.klix.backend.enums.AiContainer;
import com.klix.backend.exceptions.StorageException;
import com.klix.backend.model.IdPicture;
import com.klix.backend.repository.IdPictureRepository;
import com.klix.backend.service.DockerContainerService;
import com.klix.backend.service.JsonRequestService;
import com.klix.backend.service.picture.PictureUploadService;
import com.klix.backend.viewmodel.PictureBaseViewModel;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import lombok.extern.slf4j.Slf4j;


/**
 * Controller für unsere Entwicklungstests u.a.
 */
@Slf4j
@Controller
public class DevelopController
{
    @Autowired private PictureUploadService pictureUploadService;

    @Autowired private IdPictureRepository idPictureRepository;

    @Autowired private MessageSource messageSource;

    // Model für die Darstellung der Lokalisierungsergebnisse im View
    private PictureBaseViewModel picturebaseviewmodel = new PictureBaseViewModel(new IdPicture(), "");

    @Autowired private DockerContainerService dockerContainerService;

    @Autowired private JsonRequestService jsonRequestService;


    /**
     * Get: Upload 
     * 
     * @return Die Thymeleaf "develop/files/uploadForm" Seite
     * @throws IOException
     */
    @GetMapping("/develop/upload")
    public String showFileUpload(Model model) throws IOException
    {
        List<IdPicture> retrievedImages = idPictureRepository.findAll();
        int length = retrievedImages.size();
        IdPicture retrievedImage = null;
        
        if (length > 0) {
            retrievedImage = retrievedImages.get(length-1);
        }

        PictureBaseViewModel localizedImage = picturebaseviewmodel;

        Map<String, Boolean> isContainerUp = new HashMap<>();
        for (AiContainer container : AiContainer.values())
        {
            try {
                isContainerUp.put(container.name(), dockerContainerService.isContainerUp(container));

            } catch(Exception e) {
                log.error("error at showFileUpload Develop Controller:"+e.getMessage());
                isContainerUp.put(container.name(), false);
            }
        }
        model.addAttribute("images", retrievedImage);
        model.addAttribute("localizedImage", localizedImage);

        model.addAttribute("isContainerUp", isContainerUp);
        log.debug("is Container up to send requests to? " + isContainerUp.toString());

        return "develop/files/uploadForm";
    }


    /**
     * Post:
     * 
     * @return Die Thymeleaf "develop/files/upload" Seite per Redirect
     */
    @PostMapping("/develop/filesUpload")
    public String handleFileUpload(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes, Locale locale)
    throws IOException
    {

        if(file != null) {
            log.info("retrieved image uploaded");
            if (!file.isEmpty()){
                log.info("retrieved image is not empty");
            } else {
                log.info("retrieved image is empty");
            }
        } else {
            log.info("retrieved image not uploaded");
        }

        if (file != null)
        {
            try
            {
                pictureUploadService.storeDevPicture(file);
            } catch (StorageException e) {
                log.error(e.getMessage());

                String message = messageSource.getMessage("root.user.userFileUploadController.uploadError", null, locale);
                redirectAttributes.addFlashAttribute("message", message);
                return "redirect:/develop/upload";
            }
            String message = messageSource.getMessage("root.dev.developController.uploadSuccess", null, locale);

            redirectAttributes.addFlashAttribute("message", String.format(message, file.getOriginalFilename()));
        }
        else
        {
            String message = messageSource.getMessage("root.dev.developController.uploadError", null, locale);
            redirectAttributes.addFlashAttribute("message", message);
        }

        return "redirect:/develop/upload";
    }


    /**
     * 
     */
    @PostMapping("/develop/sendToAPI")
    public String handleFileUpload(@RequestParam("id") Long idPictureId, @RequestParam("ai-container") AiContainer container, Model model, RedirectAttributes redirectAttributes)
    {
        IdPicture retrievedImage = idPictureRepository.findById(idPictureId).orElse(null);
        if (retrievedImage == null)
        {
            redirectAttributes.addFlashAttribute("message", "Kein Bild mit der angegebenen ID gefunden.");
            return "redirect:/develop/upload";
        }


        Request request = new Request(Long.toString(idPictureId), retrievedImage.getPictureString());
        String wcResponse = null;
        try
        {
            wcResponse = jsonRequestService.sendJsonRequest(container.getUrl() + "upload", request, String.class);

        } catch (WebClientResponseException e) {
            // status code is 4xx or 5xx //

            String message = "See ai container logs for details.";
            if (e.getStatusCode().value() != 500)
            {
                JsonObject responseBody = JsonParser.parseString(e.getResponseBodyAsString()).getAsJsonObject();
                message = responseBody.get("message").getAsString();
            }

            redirectAttributes.addFlashAttribute("message", message);
            return "redirect:/develop/upload";
        } catch (Exception e)
        {
            redirectAttributes.addFlashAttribute("message", "Not connected");
            return "redirect:/develop/upload";
        }
        log.warn(wcResponse);

        List<String> messages = new ArrayList<>();
        try
        {

            // Es war nicht möglich, den JSON String als Response Object mittels objectmapper.readValue() auszulesen.
            // Es lag immer ein Deserializierungsfehler vor. Deshalb jetzt readTree().
            JsonNode root = new ObjectMapper().readTree(wcResponse);
            JsonObject jsonData = JsonParser.parseString(wcResponse).getAsJsonObject();
            
            if (jsonData.get("coordinates") == null)
            {
                redirectAttributes.addFlashAttribute("message", "No answer recieved.");
                return "redirect:/develop/upload";
            }

            // Error checks done, collect data to display //


            if (container == AiContainer.FACENET)
            {
                // get predicted_names (= ids of identified persons on the image) as string representation of their (person-)ids
                JsonElement ids = jsonData.get("predicted_names");
                String idsDisplay = "";

                // found one person
                if (!ids.isJsonArray())
                {
                    idsDisplay = ids.getAsString();

                // found more than one person
                } else {
                    List<Long> idNumbers = new ArrayList<>();
                    ids.getAsJsonArray().forEach(id -> idNumbers.add(id.getAsLong()));

                    idsDisplay = idNumbers.toString();
                }
                
                log.warn("NAMES: " + idsDisplay);
                messages.add("Person(en) erkannt als: " + idsDisplay);
            } else {
                JsonArray coordinates = jsonData.get("coordinates").getAsJsonArray();
                messages.add("Gesicht in: " + coordinates);
            }
                    
            IdPicture img = new IdPicture(retrievedImage.getName(),
                                        retrievedImage.getType(),
                                        retrievedImage.getPictureBytes());

            //Einsetzen der gerade errechnete Koordinaten in PictureView:
            picturebaseviewmodel = new PictureBaseViewModel(img, root.get("coordinates").toString());
        }
        catch (Exception e)
        {
            log.error(e.getMessage());
            e.printStackTrace();
            messages.add(e.getMessage());
        }

        redirectAttributes.addFlashAttribute("message", String.join(", ", messages));
        return "redirect:/develop/upload";
    }
}
