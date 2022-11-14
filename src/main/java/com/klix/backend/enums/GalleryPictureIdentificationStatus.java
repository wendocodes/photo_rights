package com.klix.backend.enums;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import com.klix.backend.model.GalleryPictureFaceFrame;

public enum GalleryPictureIdentificationStatus {
    
	AT_LEAST_ONE_PERSON_NOT_IN_KLIX,
	AT_LEAST_ONE_PERSON_UNKNOWN_BY_EMPLOYEES,
	EMPLOYEES_NOT_FINISHED,
	ALL_FRAMES_CONFIRMED,	
	KI_WORK_NOT_DONE_NOT_REVIEWED;

	public static GalleryPictureIdentificationStatus createGalleryPictureIdentificationStatus (SortedSet<GalleryPictureFaceFrameIdentificationStatus> set) {

		if (set!=null && set.size()>0) {
			switch (set.first()) {//Was ist das schlechteste Frame?
				case NOT_IN_KLIX : return AT_LEAST_ONE_PERSON_NOT_IN_KLIX;
				case UNKNOWN_BY_EMPLOYEES : return AT_LEAST_ONE_PERSON_UNKNOWN_BY_EMPLOYEES;
				case NO_SUGGESTION:
				case KI_WORK_DONE_NOT_REVIEWED:
				case DEFAULT: return EMPLOYEES_NOT_FINISHED;
				case KI_SUGGESTION_ACCEPTED:
				case SELF_SEARCHED: return ALL_FRAMES_CONFIRMED;
				case KI_WORK_NOT_DONE_NOT_REVIEWED: {
					for (GalleryPictureFaceFrameIdentificationStatus status : set) {
						if (!status.equals(GalleryPictureFaceFrameIdentificationStatus.KI_WORK_NOT_DONE_NOT_REVIEWED)) {
							return EMPLOYEES_NOT_FINISHED;
						}
					}
					return KI_WORK_NOT_DONE_NOT_REVIEWED;
				}
			}
		}
		return null;
	}

	public static GalleryPictureIdentificationStatus createGalleryPictureIdentificationStatus(List<GalleryPictureFaceFrame> faceFrames) {
		
    	if (faceFrames==null) return null;
		TreeSet<GalleryPictureFaceFrameIdentificationStatus> set
    			=new TreeSet<GalleryPictureFaceFrameIdentificationStatus>();
		for (GalleryPictureFaceFrame faceFrame : faceFrames) set.add(faceFrame.getIdentificationStatus());
		return createGalleryPictureIdentificationStatus(set);
	}
}
