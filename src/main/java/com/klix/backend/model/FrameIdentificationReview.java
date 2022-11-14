package com.klix.backend.model;

import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedSet;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import com.klix.backend.enums.GalleryPictureFaceFrameIdentificationStatus;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 
 * @author Andreas
 * @since Dezember 2021
 */
@Entity
@Getter
@Setter
@ToString
public class FrameIdentificationReview implements Comparable<FrameIdentificationReview> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
	private Long frameID, reviewerID;
	@Enumerated(EnumType.STRING)
	private GalleryPictureFaceFrameIdentificationStatus status=GalleryPictureFaceFrameIdentificationStatus.DEFAULT;
	private Long personID=null;//Gibts nur wenn Person von Reviewer bestätigt
	private Long timeStamp;
	
	public FrameIdentificationReview () {
	
	}
	
	public FrameIdentificationReview (Long frameID, Long reviewerID) {
		
		this.frameID=frameID;
		this.reviewerID=reviewerID;
		timeStamp=System.currentTimeMillis();
	}

	public FrameIdentificationReview(Long frameID, Long reviewerID, Long personID) {
		
		this(frameID, reviewerID);
		this.personID = personID;
	}

	public FrameIdentificationReview(Long frameID, Long reviewerID, GalleryPictureFaceFrameIdentificationStatus status) {
		
		this(frameID, reviewerID);
		this.status = status;
	}
	
	public boolean isUnknownByEmployee () {
		
		return status==GalleryPictureFaceFrameIdentificationStatus.UNKNOWN_BY_EMPLOYEES;
	}
	
	public boolean isNotInKlix () {
		
		return status==GalleryPictureFaceFrameIdentificationStatus.NOT_IN_KLIX;
	}

	@Override
	public int compareTo(FrameIdentificationReview review) {
		
		if (timeStamp==review.timeStamp) return (int)(id-review.id);//SEHR unwahrscheinlich
		if (timeStamp>review.timeStamp) return 1;
		else if (timeStamp<review.timeStamp) return -1;
		return 0;
	}
	
	public boolean equals (Object o) {
		
		if (o instanceof FrameIdentificationReview) return compareTo((FrameIdentificationReview)o)==0;
		return false;
	}
	
	public static interface ReviewsComparator extends Comparator<SortedSet<FrameIdentificationReview>> {
		
	}
	
	public static class ReviewsDefaultComparator implements ReviewsComparator {
		
		public int compare (SortedSet<FrameIdentificationReview> reviews1, SortedSet<FrameIdentificationReview> reviews2) {
			
			Iterator<FrameIdentificationReview> it1=reviews1.iterator();
			Iterator<FrameIdentificationReview> it2=reviews2.iterator();
			int result=0;
			while (it1.hasNext() && it2.hasNext()) {
				result=it1.next().compareTo(it2.next());
				if (result!=0) return result;
			}
			if (it1.hasNext()) return 1;
			if (it2.hasNext()) return -1;
			return 0;
		}
	}
}
