package com.klix.backend.model;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotNull;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@Entity

public class GalleryPictureFaceFrameCosineDistances
		implements Comparable<GalleryPictureFaceFrameCosineDistances>{//Comparable: Andreas 18.12.21

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(cascade =CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "FACE_FRAME_ID")
    //private GalleryPictureFaceFrame face_frame;//Andreas
    private GalleryPictureFaceFrame faceFrame;

    @NotNull
    private Long person_id;

    private Double cosineDistance;

    public GalleryPictureFaceFrameCosineDistances(Long person_id,  Double cosineDistance, GalleryPictureFaceFrame face_frame) {
        this.person_id = person_id;
        this.cosineDistance = cosineDistance;
        this.faceFrame = face_frame;
    }
    
    /**
     * @author Andreas
     * @since 18.12.2021
     */
    public String toString () {
    	
    	return "GalleryPictureFaceFrameCosineDistances: ID="+id+", person_id="+person_id;
    }

    /**
     * 
     * @param o
     * @return
     * @author Andreas
     * @since 18.12.2021
     */
	@Override
	public int compareTo(GalleryPictureFaceFrameCosineDistances o) {
		
		int value=(int)Math.signum(getCosineDistance()-o.getCosineDistance());
		if (value!=0) return value;
		if (getId()!=null) {
			if (o.getId()!=null) return (int)(getId()-o.getId());
		}
		if (getPerson_id()!=null) {
			if (o.getPerson_id()!=null) return (int)(getPerson_id()-o.getPerson_id());
		}
		return 0;
	}
	
	/**
     * @author Andreas
     * @since 18.12.2021
	 */
	public boolean equals (Object o) {
		
		return o instanceof GalleryPictureFaceFrameCosineDistances && 
				compareTo((GalleryPictureFaceFrameCosineDistances)o)==0;
	}

	/**
     * @author Andreas
     * @since 18.12.2021
	 */
	public String concordanceString () {
		return Math.round(100*(1-getCosineDistance()))+"%";
	}
}
