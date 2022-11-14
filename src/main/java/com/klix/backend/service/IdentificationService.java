package com.klix.backend.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.klix.backend.enums.GalleryImage_DetectionStatus;
import com.klix.backend.enums.GalleryPictureIdentificationStatus;
import com.klix.backend.model.GalleryPicture;
import com.klix.backend.model.GalleryPictureFaceFrame;
import com.klix.backend.repository.GalleryPictureFaceFrameRepository;
import com.klix.backend.repository.GalleryPictureRepository;

@Service
public class IdentificationService {
	
	@Autowired GalleryPictureRepository galleryPictureRepository;
	@Autowired GalleryPictureFaceFrameRepository galleryPictureFaceFrameRepository;
	
	/**
	 * Ein Mitarbeiter überarbeitet die Detektion, nachdem bereits die Identifikation bearbeitet wurde. Hier können sich verschiedene beispielhafte Szenarien für den Status des Galeriebildes ergeben:
	 *
	 * 1) Der Mitarbeiter fügt einen Frame hinzu:
	 * Falls der Status vorher grün war, verschlechtert er sich auf gelb.
	 * Falls der Status vorher orange oder schlechter war, ändert sich nichts.
	 * usw...
	 * 2) Der Mitarbeiter entfernt einen Frame:
	 * Falls der Status vorher grün war, ändert sich nichts.
	 * Falls aber der Status vorher orange war und der einzige dafür verantwortlicher Frame wurde gelöscht, so verbessert sich der Status...
	 * usw...
	 * 
	 * Daher wird hier gecheckt, ob der Identifikationsstatus noch aktuell ist und gegebenenfalls aktualisiert
	 * 
	 * @since 15.02.2022
	 * @param imageID
	 * @param faceFrameID
	 * @param identificationStatus
	 */
	public void checkStatus (GalleryPicture galleryPicture, List<GalleryPictureFaceFrame> faceFrames) {
		
		if (galleryPicture == null) { return; }

		GalleryPictureIdentificationStatus newStatus
				=GalleryPictureIdentificationStatus.createGalleryPictureIdentificationStatus(faceFrames);
		
		// no Frame
		if (newStatus == null) {

			//Wir können erst mal nichts machen. Kein Frame, aber vielleicht ist doch ein Gesicht drauf...
			if (!galleryPicture.getStatus().equals(GalleryImage_DetectionStatus.fullyReviewed)) { return; }

			//Der MA hat Detektion gecheckt. Gibt kein Gesicht. Bild kann frei gegeben werden
			newStatus = GalleryPictureIdentificationStatus.ALL_FRAMES_CONFIRMED;
		}
		if (!newStatus.equals(galleryPicture.getIdentificationStatus())) {
			galleryPicture.setIdentificationStatus(newStatus);
			//galleryPicture.setLastEdited(LocalDate.now());//Nicht. Weil evtl Zustande kommen über andere Wege als Identifikationspanel. Wenn über Identifikationspanel aktualisiert wird, wird das Datum gesetzt.
			galleryPictureRepository.save(galleryPicture);
		}
	}
	
	public void checkStatus(GalleryPicture galleryPicture) {
		if (galleryPicture == null) { return; }

		List<GalleryPictureFaceFrame> faceFrames=galleryPictureFaceFrameRepository.findByGalleryPictureId(galleryPicture.getId());
		checkStatus(galleryPicture, faceFrames);
	}
}
