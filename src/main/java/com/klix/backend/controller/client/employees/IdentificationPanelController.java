package com.klix.backend.controller.client.employees;

import java.time.LocalDate;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.klix.backend.enums.GalleryPictureFaceFrameIdentificationStatus;
import com.klix.backend.enums.GalleryPictureIdentificationStatus;
import com.klix.backend.enums.ViewAccess;
import com.klix.backend.model.FrameIdentificationReview;
import com.klix.backend.model.GalleryPicture;
import com.klix.backend.model.GalleryPictureFaceFrame;
import com.klix.backend.model.GalleryPictureFaceFrameCosineDistances;
import com.klix.backend.repository.FrameIdentificationReviewRepository;
import com.klix.backend.repository.GalleryPictureFaceFrameRepository;
import com.klix.backend.repository.GalleryPictureRepository;
import com.klix.backend.repository.GallyPictureFaceFrameCosineDistanceRepository;
import com.klix.backend.repository.IdPictureRepository;
import com.klix.backend.repository.PermissionRepository;
import com.klix.backend.repository.PersonRepository;
import com.klix.backend.repository.projections.PersonPermission;
import com.klix.backend.service.IdentificationService;
import com.klix.backend.service.PersonService;
import com.klix.backend.service.ViewAccessService;
import com.klix.backend.service.picture.PictureCropService;
import com.klix.backend.service.picture.PictureFunctionService;
import com.klix.backend.viewmodel.IdentificationPanelViewModel;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Andreas
 * @since 11.12.2021
 */
@Slf4j
@Controller
public class IdentificationPanelController {
	
	@Autowired GalleryPictureRepository galleryPictureRepository;
	@Autowired GalleryPictureFaceFrameRepository galleryPictureFaceFrameRepository;
	@Autowired GallyPictureFaceFrameCosineDistanceRepository gallyPictureFaceFrameCosineDistanceRepository;
	@Autowired IdPictureRepository idPictureRepository;
	@Autowired PersonRepository personRepository;
	@Autowired FrameIdentificationReviewRepository frameIdentificationReviewRepository;
	@Autowired ViewAccessService viewAccessService;
	@Autowired PermissionRepository permissionRepository;
	@Autowired PersonService personService;
	@Autowired IdentificationService identificationService;
	
	private static final int LAST_REVIEWS_QUANTITY = 1, MAX_NUMBER_OF_BEST_SUGGESTIONS = 5;
	private static final double MAX_DISTANCE=0.7;
	
	@GetMapping("identificationPanel/{clientID}/{roleID}/{imageID}")
	public String getIdentificationPanel (@PathVariable("clientID") Long clientID,
			@PathVariable("roleID") Long roleID, @PathVariable("imageID") Long imageID, Model model) {

        String viewName = "client/mitarbeiter/identificationPanel";
        if (!setPermission(clientID, viewName, roleID, model)) return "errors/access-denied";
		
		GalleryPicture galleryPicture = galleryPictureRepository.findById(imageID).get();
		model.addAttribute("galleryPicture", galleryPicture);
		List<GalleryPictureFaceFrame> faceFrames=galleryPictureFaceFrameRepository.findByGalleryPictureId(imageID);
		identificationService.checkStatus(galleryPicture, faceFrames);
		if (faceFrames!=null) {
			setPictureStrings(galleryPicture, faceFrames);
			model.addAttribute("faceFrames", faceFrames);
			LinkedList<IdentificationPanelViewModel> identificationPanelViewModels=new LinkedList<IdentificationPanelViewModel>();
			for (GalleryPictureFaceFrame faceFrame : faceFrames) {
				IdentificationPanelViewModel identificationPanelViewModel=new IdentificationPanelViewModel(faceFrame);
				identificationPanelViewModel.setPersonRepository(personRepository);
				identificationPanelViewModel.setIdPictureRepository(idPictureRepository);
				Set<GalleryPictureFaceFrameCosineDistances> set=gallyPictureFaceFrameCosineDistanceRepository.findAllByFaceFrame(faceFrame);
				for (GalleryPictureFaceFrameCosineDistances distance : set) {
					identificationPanelViewModel.put(distance,
							personRepository.findById(distance.getPerson_id()).orElse(null),
							idPictureRepository.findByPersonId(distance.getPerson_id()).orElse(null));
				}
				identificationPanelViewModel.addReviews(lastOf(frameIdentificationReviewRepository
						.findAllByFrameID(faceFrame.getFaceFrameId()),LAST_REVIEWS_QUANTITY));
				identificationPanelViewModel.filter(MAX_DISTANCE, MAX_NUMBER_OF_BEST_SUGGESTIONS);
				identificationPanelViewModel.setLink("/suggestion/"+clientID+"/"+roleID+"/"+imageID+"/"+faceFrame.getFaceFrameId()+"/");
				identificationPanelViewModels.add(identificationPanelViewModel);
			}
			model.addAttribute("identificationPanelViewModels", identificationPanelViewModels);
			model.addAttribute("galleryPictureStatusColor", createColor(galleryPicture.getIdentificationStatus()));
		}
		else log.info("Keine Frames für dieses Galeriebild");
		return viewName;
	}
	
	@GetMapping("suggestion/{clientID}/{roleID}/{imageID}/{faceFrameID}/{personID}")
	public String saveReviewPerson (@PathVariable("clientID") Long clientID,
			@PathVariable("roleID") Long roleID, @PathVariable("imageID") Long imageID,
			@PathVariable("faceFrameID") Long faceFrameID, @PathVariable("personID") Long personID, Model model) {

		if (permissionRepository.findPermissionByPersonAndClientAndRole(personService
        		.getPersonId(), clientID, roleID).orElse(null)==null) return "errors/access-denied";
		frameIdentificationReviewRepository.save(new FrameIdentificationReview(faceFrameID, personService
        		.getPersonId(), personID) );
		updateStatuses(imageID, faceFrameID, GalleryPictureFaceFrameIdentificationStatus.KI_SUGGESTION_ACCEPTED);
		return "redirect:/identificationPanel/"+clientID+"/"+roleID+"/"+imageID;
	}

	@GetMapping("suggestion/{clientID}/{roleID}/{imageID}/{faceFrameID}/unknown")
	public String saveReviewUnknown (@PathVariable("clientID") Long clientID,
			@PathVariable("roleID") Long roleID, @PathVariable("imageID") Long imageID,
			@PathVariable("faceFrameID") Long faceFrameID, Model model) {

		if (permissionRepository.findPermissionByPersonAndClientAndRole(personService
        		.getPersonId(), clientID, roleID).orElse(null)==null) return "errors/access-denied";
		frameIdentificationReviewRepository.save(new FrameIdentificationReview(faceFrameID, personService
        		.getPersonId(), GalleryPictureFaceFrameIdentificationStatus.UNKNOWN_BY_EMPLOYEES));
		updateStatuses(imageID, faceFrameID, GalleryPictureFaceFrameIdentificationStatus.UNKNOWN_BY_EMPLOYEES);
		return "redirect:/identificationPanel/"+clientID+"/"+roleID+"/"+imageID;
	}
	
	@GetMapping("suggestion/{clientID}/{roleID}/{imageID}/{faceFrameID}/notInKlix")
	public String saveReviewNotInKlix (@PathVariable("clientID") Long clientID,
			@PathVariable("roleID") Long roleID, @PathVariable("imageID") Long imageID,
			@PathVariable("faceFrameID") Long faceFrameID, Model model) {

		if (permissionRepository.findPermissionByPersonAndClientAndRole(personService
        		.getPersonId(), clientID, roleID).orElse(null)==null) return "errors/access-denied";
		frameIdentificationReviewRepository.save(new FrameIdentificationReview(faceFrameID, personService
        		.getPersonId(), GalleryPictureFaceFrameIdentificationStatus.NOT_IN_KLIX));
		updateStatuses(imageID, faceFrameID, GalleryPictureFaceFrameIdentificationStatus.NOT_IN_KLIX);
		return "redirect:/identificationPanel/"+clientID+"/"+roleID+"/"+imageID;
	}
	
	public void updateStatuses (Long imageID, Long faceFrameID, GalleryPictureFaceFrameIdentificationStatus identificationStatus) {
		
		GalleryPictureFaceFrame faceFrame=galleryPictureFaceFrameRepository.findById(faceFrameID).orElse(null);
		if (faceFrame!=null) {
			if (faceFrame.getIdentificationStatus()==null || !faceFrame.getIdentificationStatus().equals(identificationStatus)) {
				faceFrame.setIdentificationStatus(identificationStatus);
				galleryPictureFaceFrameRepository.save(faceFrame);
				GalleryPicture galleryPicture = galleryPictureRepository.findById(imageID).orElse(null);
				if (galleryPicture!=null) {
					List<GalleryPictureFaceFrame> faceFrames=galleryPictureFaceFrameRepository.findByGalleryPictureId(imageID);
					if (faceFrames!=null) {
						GalleryPictureIdentificationStatus newStatus
								=GalleryPictureIdentificationStatus.createGalleryPictureIdentificationStatus(faceFrames);
						if (!newStatus.equals(galleryPicture.getIdentificationStatus())) {
							galleryPicture.setIdentificationStatus(newStatus);
							galleryPicture.setLastEdited(LocalDate.now());
							galleryPictureRepository.save(galleryPicture);
						}
					}
				}
			}
		}
	}
	

	
	public String createColor (GalleryPictureIdentificationStatus identificationStatus) {
		
		if (identificationStatus==null) return "yellow";
		switch (identificationStatus) {
			case AT_LEAST_ONE_PERSON_NOT_IN_KLIX: return "danger";
			case AT_LEAST_ONE_PERSON_UNKNOWN_BY_EMPLOYEES: return "warning";
			case EMPLOYEES_NOT_FINISHED : return "yellow";
			case ALL_FRAMES_CONFIRMED : return "success";
			case KI_WORK_NOT_DONE_NOT_REVIEWED : return "secondary";
		}
		return null;
	}

	public List<FrameIdentificationReview> lastOf (List<FrameIdentificationReview> set, int quantity) {
		
		TreeSet<FrameIdentificationReview> timeSorted=new TreeSet<FrameIdentificationReview>(set);
		LinkedList<FrameIdentificationReview> result=new LinkedList<FrameIdentificationReview>();
		for (int i=0; i<quantity && timeSorted.size()>0; i++) result.add(timeSorted.pollLast());
		return result;
	}

 
    @Autowired PictureCropService pictureCropService;
    
	private void setPictureStrings (GalleryPicture galleryPicture, List<GalleryPictureFaceFrame> faceFrames) {
		
		byte[] galleryPictureBytes=PictureFunctionService.decompressBytes(galleryPicture.getPictureBytes());
		for (GalleryPictureFaceFrame faceFrame : faceFrames) {
			faceFrame.setPictureString(Base64.getEncoder().encodeToString(pictureCropService
					.cropImage(galleryPictureBytes,	faceFrame.getCoordinates())));
		}
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