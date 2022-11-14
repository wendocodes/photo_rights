package com.klix.backend.viewmodel;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.TreeSet;

import org.springframework.context.i18n.LocaleContextHolder;

import com.klix.backend.model.GalleryPictureFaceFrame;
import com.klix.backend.model.IdPicture;

import lombok.Getter;

@Getter
public class PersonSearchViewModel {
	
	String noChoiceLink;
	GalleryPictureFaceFrame faceFrame;
	TreeSet<PersonToChoose> personsToChoose=new TreeSet<PersonToChoose>();
	TreeSet<PersonToChoose> personsToChooseNameSorted=new TreeSet<PersonToChoose>(new Comparator<PersonToChoose>() {
		@Override
		public int compare(PersonToChoose o1, PersonToChoose o2) {
			int result=o1.getLastName().compareTo(o2.getLastName());
			if (result!=0) return result;
			result=o1.getFirstName().compareTo(o2.getFirstName());
			if (result!=0) return result;
			long tmp=o1.getId()-o2.getId();
			if (tmp>0) return 1;
			if (tmp<0) return -1;
			return 0;
		}
	});
	
	public PersonSearchViewModel(GalleryPictureFaceFrame faceFrame) {
		
		this.faceFrame=faceFrame;
	}
	
	public static final IdPicture noPictureAvailable=new IdPicture ("noPictureAvailable", "image/png;base64", null) {
	   public String getPictureString() {
		   if (LocaleContextHolder.getLocale().toString().equals("de")) {//Es stimmt was nicht mit Locale. Es gi
			   return "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";
		   	}
		   else {
			   return "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";
		   }
		}
		public String getKiPictureString() {
		   return getPictureString();
	   }
	};
			   
	public boolean add (PersonToChoose ptc) {
		
		if (personsToChoose.add(ptc)) return personsToChooseNameSorted.add(ptc);
		return false;
	}
	
	public boolean containsPersonID (Long personID) {
		
		return false;
	}

	@Override
	public String toString() {
		return "PersonSearchViewModel [personsToChoose=" + personsToChoose + "]";
	}
	

	public void setNoChoiceLink(String noChoiceLink) {
		this.noChoiceLink = noChoiceLink;
	}
	
	@Getter
	public static class PersonToChoose implements Comparable <PersonToChoose> {
		
		Long id;
		String firstName, lastName, choiceLink;
		static String unknownName="Name unbekannt";
		boolean isInChildRepository;
		LocalDate birthdate;
		IdPicture idPicture;

		public PersonToChoose (Long id, String firstName, String lastName, boolean isInChildRepository,
				LocalDate birthdate, IdPicture idPicture, String choiceLink) {
			
			super();
			this.id = id;
			if (firstName!=null) this.firstName = firstName;
			else this.firstName=unknownName;
			if (lastName!=null) this.lastName = lastName;
			else this.lastName=unknownName;
			this.isInChildRepository = isInChildRepository;
			this.birthdate = birthdate;
			if (idPicture!=null ) this.idPicture = idPicture;
			else this.idPicture=noPictureAvailable;
			this.choiceLink=choiceLink;
		}
		
		@Override
		public int compareTo (PersonToChoose o) {
			
			if (id<o.id) return -1;
			if (id>o.id) return 1;
			return 0;
		}
	}
}
