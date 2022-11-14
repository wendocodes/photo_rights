package com.klix.backend.controller.client.employees;

import java.util.Base64;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.klix.backend.enums.GalleryPictureFaceFrameIdentificationStatus;
import com.klix.backend.enums.ViewAccess;
import com.klix.backend.model.FrameIdentificationReview;
import com.klix.backend.model.GalleryPictureFaceFrame;
import com.klix.backend.model.IdPicture;
import com.klix.backend.model.Person;
import com.klix.backend.repository.FrameIdentificationReviewRepository;
import com.klix.backend.repository.GalleryPictureFaceFrameRepository;
import com.klix.backend.repository.GalleryPictureRepository;
import com.klix.backend.repository.GallyPictureFaceFrameCosineDistanceRepository;
import com.klix.backend.repository.IdPictureRepository;
import com.klix.backend.repository.PermissionRepository;
import com.klix.backend.repository.PersonRepository;
import com.klix.backend.repository.projections.PersonPermission;
import com.klix.backend.service.PersonService;
import com.klix.backend.service.ViewAccessService;
import com.klix.backend.service.picture.PictureCropService;
import com.klix.backend.service.picture.PictureFunctionService;
import com.klix.backend.viewmodel.PersonSearchViewModel;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Andreas
 * @since 10.01.2022
 */
@Slf4j
@Controller
public class PersonSearchController {
	
	@Autowired GalleryPictureRepository galleryPictureRepository;
	@Autowired GalleryPictureFaceFrameRepository galleryPictureFaceFrameRepository;
	@Autowired GallyPictureFaceFrameCosineDistanceRepository gallyPictureFaceFrameCosineDistanceRepository;
	@Autowired IdPictureRepository idPictureRepository;
	@Autowired PersonRepository personRepository;
	@Autowired FrameIdentificationReviewRepository frameIdentificationReviewRepository;
	@Autowired ViewAccessService viewAccessService;
	@Autowired PermissionRepository permissionRepository;
	@Autowired PersonService personService;
	
	@GetMapping("suggestion/{clientID}/{roleID}/{imageID}/{faceFrameID}/searchPerson")
	public String getPersonSearchPanel (@PathVariable("clientID") Long clientID,
			@PathVariable("roleID") Long roleID, @PathVariable("imageID") Long imageID,
			@PathVariable("faceFrameID") Long faceFrameID, Model model) {//, Locale locale) {

        String viewName = "client/mitarbeiter/personSearchPanel";
        if (!setPermission(clientID, viewName, roleID, model)) return "errors/access-denied";
        Set<Person> childs=personRepository.findChildrenByClientId(clientID);
		List<PersonPermission> personPermissions=permissionRepository.findPermissionByClientId(clientID);
		//Wir haben im personRepository zwei Listen von Personen: childs und persons. Wie wurde das programmiert? Kann es hier überschneidungen geben?
		GalleryPictureFaceFrame faceFrame=galleryPictureFaceFrameRepository.findById(faceFrameID).orElse(null);
		faceFrame.setGallery_picture(galleryPictureRepository.findById(imageID).get());
		setPictureString(faceFrame); 
		PersonSearchViewModel personSearchViewModel=new PersonSearchViewModel(faceFrame);
		for (Person child : childs) {
			IdPicture idPicture=idPictureRepository.findByPersonId(child.getId()).orElse(null);
			String choiceLink="/selfSearched/"+clientID+"/"+roleID+"/"+imageID+"/"+faceFrameID+"/"+child.getId();
			personSearchViewModel.add(new PersonSearchViewModel.PersonToChoose(child.getId(), child.getFirstName(), child.getLastName(),
					true, child.getBirthdate(),	idPicture, choiceLink));
		}
		for (PersonPermission personPermission : personPermissions) {
			Long personID=personPermission.getPerson_id();
			Person person=personRepository.findById(personID).orElse(null);
			if (person!=null) {
				IdPicture idPicture=idPictureRepository.findByPersonId(personID).orElse(null);
				String choiceLink="/selfSearched/"+clientID+"/"+roleID+"/"+imageID+"/"+faceFrameID+"/"+personID;
				personSearchViewModel.add(new PersonSearchViewModel.PersonToChoose(personID, person.getFirstName(), person.getLastName(), 
						false, person.getBirthdate(), idPicture, choiceLink));
			}
		}
		personSearchViewModel.setNoChoiceLink("/identificationPanel/"+clientID+"/"+roleID+"/"+imageID);
		model.addAttribute("personSearchViewModel", personSearchViewModel);
		return viewName;
	}
	
	@Autowired IdentificationPanelController identificationPanelController;
	
	@GetMapping("selfSearched/{clientID}/{roleID}/{imageID}/{faceFrameID}/{personID}")
	public String saveReviewPerson (@PathVariable("clientID") Long clientID,
			@PathVariable("roleID") Long roleID, @PathVariable("imageID") Long imageID,
			@PathVariable("faceFrameID") Long faceFrameID, @PathVariable("personID") Long personID, Model model) {

		if (permissionRepository.findPermissionByPersonAndClientAndRole(personService
        		.getPersonId(), clientID, roleID).orElse(null)==null) return "errors/access-denied";
		frameIdentificationReviewRepository.save(new FrameIdentificationReview(faceFrameID, personService
        		.getPersonId(), personID) );
		identificationPanelController.updateStatuses(imageID, faceFrameID,
				GalleryPictureFaceFrameIdentificationStatus.SELF_SEARCHED);
		return "redirect:/identificationPanel/"+clientID+"/"+roleID+"/"+imageID;
	}
        
    @Autowired PictureCropService pictureCropService;
    
	private void setPictureString (GalleryPictureFaceFrame faceFrame) {
		
		byte[] galleryPictureBytes=PictureFunctionService.decompressBytes(faceFrame.getGallery_picture().getPictureBytes());
		faceFrame.setPictureString(Base64.getEncoder().encodeToString(pictureCropService
					.cropImage(galleryPictureBytes,	faceFrame.getCoordinates())));
	} 
        
	private boolean setPermission (Long clientID, String viewName, Long roleID, Model model) {
		
        ViewAccess viewAccess = viewAccessService.checkPermissions(clientID, viewName);
	    if (viewAccess == ViewAccess.FORBIDDEN) return false;
        PersonPermission permission = permissionRepository
        		.findPermissionByPersonAndClientAndRole(personService
        		.getPersonId(), clientID, roleID).orElse(null);
        if (permission == null) return false;// if no person permissions found, deny access to this view
        model.addAttribute("permission", permission);
        return true;
	}

}
