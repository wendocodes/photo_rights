package com.klix.backend.enums;

public enum GalleryPictureFaceFrameIdentificationStatus {

	//Die Reihenfolge gibt eine Ordnung vor! Ordnung: Aufsteigend. NICHT ÄNDERN!
	//Die Ordnung bestimmt den Status des Galeriebildes
	NOT_IN_KLIX,//Wenn ein Frame nicht in Klix ist, dominiert dies den Status des Galeriebildes - Das Galeriebild bekommt rot
	UNKNOWN_BY_EMPLOYEES,//Wenn alle Frames in Klix sind, dominiert dieses Frame den Status des Galeriebildes - Das Galeriebild bekommt orange
	NO_SUGGESTION, KI_WORK_DONE_NOT_REVIEWED, KI_WORK_NOT_DONE_NOT_REVIEWED, DEFAULT,//= Mitarbeiter noch nicht fertig. Wenn dies der niedrigste Status ist, bekommt das Galeriebild gelb
	KI_SUGGESTION_ACCEPTED, SELF_SEARCHED;//Wenn dies der niedrigste Status ist, wurden alle Frames Personen zugeordnet, daher bekommt das Galeriebild grün
	
	//Ausnahme:	KI_WORK_NOT_DONE_NOT_REVIEWED;Wenn alle FaceFrames diesen Status haben

}
