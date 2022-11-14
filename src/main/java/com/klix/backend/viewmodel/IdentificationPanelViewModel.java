package com.klix.backend.viewmodel;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import com.klix.backend.enums.GalleryPictureFaceFrameIdentificationStatus;
import com.klix.backend.model.FrameIdentificationReview;
import com.klix.backend.model.GalleryPictureFaceFrame;
import com.klix.backend.model.GalleryPictureFaceFrameCosineDistances;
import com.klix.backend.model.IdPicture;
import com.klix.backend.model.Person;
import com.klix.backend.repository.IdPictureRepository;
import com.klix.backend.repository.PersonRepository;

import lombok.Getter;

@Getter
/**
 * 
 * @author Andreas
 * @since 18.12.2021
 */
public class IdentificationPanelViewModel {
	
	//Das ist die Frage: Ob die Repositories "erlaubt" sind.
	PersonRepository personRepository;
	IdPictureRepository idPictureRepository;
	
	private GalleryPictureFaceFrame galleryPictureFaceFrame;
	private TreeSet<Suggestion> suggestions=new TreeSet<Suggestion>();
	String link;
	
	public IdentificationPanelViewModel (GalleryPictureFaceFrame galleryPictureFaceFrame) {
		
		this.galleryPictureFaceFrame=galleryPictureFaceFrame;
	}
	
	public void setPersonRepository(PersonRepository personRepository) {
		this.personRepository = personRepository;
	}

	public void setIdPictureRepository(IdPictureRepository idPictureRepository) {
		this.idPictureRepository = idPictureRepository;
	}
	
	public boolean put (GalleryPictureFaceFrameCosineDistances distance, Person person,
			IdPicture idPicture) {
		
		return suggestions.add(new Suggestion(distance, person, idPicture));
	}
	
	public void addReviews (List<FrameIdentificationReview> reviews) {

		for (FrameIdentificationReview review : reviews) {
			boolean assigned=false;
			for (Suggestion suggestion : suggestions) {//Bei den SEHR kurzen Listen, kann man O(n*m) erlauben
				if ((review.isNotInKlix() && suggestion instanceof NotInKlix)
						|| (review.isUnknownByEmployee() && suggestion instanceof UnknownByEmployees)
						|| (suggestion.getPerson()!=null && (suggestion.getPerson().getId()==review.getPersonID()))) {
					suggestion.addReview(review);
					assigned=true;
					break;
				}
			}
			if (!assigned) {
				Suggestion suggestion=null;
				if (review.isNotInKlix()) suggestion=new NotInKlix();
				else if	(review.isUnknownByEmployee()) suggestion=new UnknownByEmployees();
				else {//Es gibt einen "review", in dem sich für eine Person entschieden wurde, für die es keinen Klix-Vorschlag gibt (nur die fünf besten sind in der Datenbank)
					IdPicture ip=idPictureRepository.findByPersonId(review.getPersonID()).orElse(null);
					if (ip==null) ip=PersonSearchViewModel.noPictureAvailable;
					suggestion=new SelfSearched(personRepository.findById(review.getPersonID()).orElse(null), ip);
				}
				if (suggestion!=null) suggestion.addReview(review);
			}
		}
	}
	
	public void filter (double maxDistance, int numberOfBest) {
		
		TreeSet<Suggestion> newSuggestions=new TreeSet<Suggestion>();
		//int counter=0;
		for (Suggestion suggestion : suggestions) {
			if (suggestion.getNumberOfReviews()>0 || suggestion.getDistance().getCosineDistance()<=maxDistance) {
				newSuggestions.add(suggestion);
			}
		}
		suggestions=new TreeSet<Suggestion>();
		int counter=0;
		for (Suggestion suggestion : newSuggestions) {
			if (counter++<numberOfBest) suggestions.add(suggestion);
			else break;
		}
	}
	
	public void setLink (String link) {
		
		this.link=link;
	}
	
	public GalleryPictureFaceFrameIdentificationStatus createFrameStatus () {

		if (getNumberOfSuggestions()==0) return GalleryPictureFaceFrameIdentificationStatus.NO_SUGGESTION;
		Suggestion bestSuggestion=suggestions.first();
		if (bestSuggestion.isKIAssignedAndConfirmed()) return GalleryPictureFaceFrameIdentificationStatus.KI_SUGGESTION_ACCEPTED;
		if (bestSuggestion.isNotInKlix()) return GalleryPictureFaceFrameIdentificationStatus.NOT_IN_KLIX;
		if (bestSuggestion.isUnknownByEmployees()) return GalleryPictureFaceFrameIdentificationStatus.UNKNOWN_BY_EMPLOYEES;
		if (bestSuggestion.isSelfSearched()) return GalleryPictureFaceFrameIdentificationStatus.SELF_SEARCHED;
		return GalleryPictureFaceFrameIdentificationStatus.DEFAULT;
	}
	
	public String createColor() {
		
		switch (createFrameStatus()) {
			case KI_SUGGESTION_ACCEPTED :
			case SELF_SEARCHED :	return "success";
			case NOT_IN_KLIX : return "danger";
			case UNKNOWN_BY_EMPLOYEES : return "warning";
			default : return "light";
		}
	}
	
	public int getNumberOfSuggestions () {
		
		if (suggestions==null) return 0;
		return suggestions.size();
	}
	
	public String toString () {
		
		return "galleryPictureFaceFrame="+galleryPictureFaceFrame+"\nsuggestions="+suggestions;
	}
	
	/**
	 * Ein Vorschlag stellt eine Beziehung zwischen dem GalleryPictureFaceFrame und EINER Person her.
	 * Es kann aber zu dieser einen Person mehrere Reviewer geben, die diese Person vorgeschlagen haben.
	 * Es bleibt dann bei einem Vorschlag, der die Zustimmung mehrer Reviewer hat.
	 * Ebenso kann es zu einem Frame mehrere verschiedene Vorschläge geben.
	 * @author Andreas
	 *
	 */
	@Getter
	public class Suggestion implements Comparable<Suggestion> {
		
		private GalleryPictureFaceFrameCosineDistances distance=null;//oft nicht null
		private Person person=null;//oft nicht null
		private IdPicture idPicture=null;
		private SortedSet<FrameIdentificationReview> reviews=null;//meistens null
		private String type="Unknown";
		private FrameIdentificationReview.ReviewsComparator reviewsComparator=new FrameIdentificationReview.ReviewsDefaultComparator();
		
		public Suggestion (GalleryPictureFaceFrameCosineDistances distance, Person person, IdPicture idPicture) {

			this(person, idPicture);
			this.distance = distance;
			this.person = person;
			this.idPicture = idPicture;
			type="KI";
		}

		public Suggestion (Person person, IdPicture idPicture) {

			this.person = person;
			this.idPicture = idPicture;
		}
		
		public Suggestion () {

		}
		
		public void setType (String type) {
			
			this.type=type;
		}
		
		public boolean addReview (FrameIdentificationReview review) {
			
			suggestions.remove(this);
			if (reviews==null) reviews=new TreeSet<FrameIdentificationReview>();
			boolean result=reviews.add(review);
			suggestions.add(this);
			return result;
		}
		
		public boolean setReviewers (SortedSet<FrameIdentificationReview> reviews) {
			
			suggestions.remove(this);
			this.reviews=reviews;
			return suggestions.add(this);
		}
		
		public int getNumberOfReviews () {
			
			if (reviews==null) return 0;
			return reviews.size();
		}
		
		/*
		 * @since 15.03.22
		 */
		public IdPicture getIdPicture () {
			
			if (idPicture!=null) return idPicture;
			return PersonSearchViewModel.noPictureAvailable;
		}
		
		public int compareTo (Suggestion suggestion) {
			
			int result=0;
			if (getNumberOfReviews()>0) {
				if (suggestion.getNumberOfReviews()==0) return -1;
				result=reviewsComparator.compare(suggestion.getReviews(), getReviews());//welche reviews haben Vorrang?
				if (result!=0) return -result;
				//Gleich gute Reviews / Unwahrscheinlich nach Zeitstempel
				if (suggestion instanceof UnknownByEmployees && !(this instanceof UnknownByEmployees)) return 1;
				if (!(suggestion instanceof UnknownByEmployees) && this instanceof UnknownByEmployees) return -1;
				if (suggestion instanceof NotInKlix && !(this instanceof NotInKlix)) return 1;
				if (!(suggestion instanceof NotInKlix) && this instanceof NotInKlix) return -1;
			}
			else if (getNumberOfReviews()==0 && suggestion.getNumberOfReviews()>0) return 1;
			if (distance!=null) {
				if (suggestion.distance==null) return -1;
				result=distance.compareTo(suggestion.distance);
				if (result!=0) return result;
			}
			else if (suggestion.distance!=null) return 1;
			return 0;
		}
		
		public boolean equals (Object o) {
			
			if (o instanceof Suggestion) return compareTo((Suggestion)o)==0;
			return false;
		}
		
		public String getInfo () {
			
			StringBuffer info=new StringBuffer();
			if (distance!=null) info.append("Klix-KI: "+distance.concordanceString());
			//else info.append("Keine KI Empfehlung");
			if (reviews!=null && reviews.size()>0) {
				if (info.length()>0) info.append("<br>");
				info.append("Bestätigt durch:");
				for (FrameIdentificationReview review : reviews) {
					Person reviewer=personRepository.findById(review.getReviewerID()).orElse(null);
					if (reviewer!=null) info.append("<br>"+reviewer.getFirstName()+" "+reviewer.getLastName());
					else info.append("<br>unbekannt");
				}
			}
			return info.toString();
		}
		
		public boolean isReviewed () {
			
			return getNumberOfReviews()>0;
		}
		
		public boolean isKIAssignedAndConfirmed () {
			
			return type.equals("KI") && isReviewed();
		}
		
		public boolean isKIAssignedAndNotReviewed () {
			
			return type.equals("KI") && !isReviewed();
		}
		
		public boolean isNotInKlix () {
			
			return this instanceof NotInKlix && isReviewed();
		}
		
		public boolean isUnknownByEmployees () {
			
			return this instanceof UnknownByEmployees && isReviewed();
		}
		
		
		public boolean isSelfSearched () {
			
			return this instanceof SelfSearched;
		}
		
		public boolean isAssignedAndConfirmed () {
			
			return isKIAssignedAndConfirmed() || isSelfSearched();
		}
		
		public String getLink () {

			//if (type.equals("NotInKlix") || type.equals("UnknownByEmployees")) return link+type;Korrektur Bug 26006 Andreas 23.03.22
			if (type.equals("NotInKlix")) return link+"notInKlix";//Korrektur Bug 26006
			if (type.equals("UnknownByEmployees")) return link+"unknown";//Korrektur Bug 26006
			return link+person.getId();
		}
		
		public String toString() {
			
			return "Suggestion: [reviews="+reviews+", type="+type+ (!(type.equals("NotInKlix") || type.equals("UnknownByEmployees")) ?
					", personID="+person.getId()+", personLastName="+person.getLastName()+", idPictureID="+idPicture.getId() : "")+"]";
		}
		
		public String getCardText () {
			
			switch (type) {
				case "NotInKlix" : return "Nicht in Klix";
				case "UnknownByEmployees" : return "Unbekannt";
				default : return person.getFirstName()+" "+person.getLastName();
			}
		}
	}
	
	public class NotInKlix extends Suggestion {
		
		public NotInKlix () {

			setType("NotInKlix");
		}
	}
	
	public class UnknownByEmployees extends Suggestion {
		
		public UnknownByEmployees () {

			setType("UnknownByEmployees");
		}
	}
	
	public class SelfSearched extends Suggestion {
		
		public SelfSearched (Person person, IdPicture idPicture) {

			super(person, idPicture);
			setType("SelfSearched");
		}
	}
}
