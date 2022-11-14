package com.klix.backend.model;


import java.util.Arrays;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

import com.klix.backend.enums.FaceFrameGenerationStatus;
import com.klix.backend.enums.GalleryPictureFaceFrameIdentificationStatus;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class GalleryPictureFaceFrame 
{
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name = "FACE_FRAME_ID")
    private Long faceFrameId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "GALLERY_PICTURE_ID")
    private GalleryPicture gallery_picture;

    @Column(name = "coordinates")
    private Integer[] coordinates;

//    @Enumerated(EnumType.STRING)
//    @Column(name = "status")
//    private GalleryImage_IdentificationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "generated_by")
    private FaceFrameGenerationStatus generation_status;

    //@Enumerated(EnumType.STRING)
    private GalleryPictureFaceFrameIdentificationStatus identificationStatus;//Andreas 28.01.2022
    
    @Lob
    @Column(columnDefinition = "MEDIUMBLOB")
    private String neuralHash;
    
    @OneToMany(mappedBy = "faceFrame", fetch = FetchType.LAZY)
    private Set<GalleryPictureFaceFrameCosineDistances> cosineDistances;
    
    @Transient
    private String pictureString;

    @Lob
    @Column(columnDefinition = "MEDIUMBLOB")
    private byte[] blurryFaceFramePixels;


    public GalleryPictureFaceFrame(
        Integer[] coordinates,
        String neuralHash,
        GalleryPicture img,
        byte[] blurryFaceFramePixels,
        FaceFrameGenerationStatus generationStatus,
        GalleryPictureFaceFrameIdentificationStatus identificationStatus
    )
    {
        this.setCoordinates(coordinates);
        this.setNeuralHash(neuralHash);
        this.setGallery_picture(img);
        this.setBlurryFaceFramePixels(blurryFaceFramePixels);
        this.setGeneration_status(generationStatus);
        this.setIdentificationStatus(identificationStatus);
    }

    
    /**
     * @author Andreas
     * @since 19.02.2022
     * @return
     */
    public GalleryPictureFaceFrameIdentificationStatus getIdentificationStatus () {
    	
    	if (identificationStatus==null) return GalleryPictureFaceFrameIdentificationStatus.DEFAULT;
    	return identificationStatus;
    }
    
    public String toString() {
    	return "GalleryPictureFaceFrame, ID="+faceFrameId;
    }
    /**
     * @author Andreas
     * @since 22.01.2022
      */
    public String getType () {
    	
    	return gallery_picture.getType();
    }
}
