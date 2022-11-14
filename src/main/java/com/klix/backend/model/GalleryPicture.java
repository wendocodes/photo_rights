package com.klix.backend.model;

import java.time.LocalDate;
import java.util.Base64;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;


import com.klix.backend.enums.GalleryImage_DetectionStatus;
import com.klix.backend.enums.GalleryPictureIdentificationStatus;
import com.klix.backend.model.interfaces.PictureInterface;
import com.klix.backend.service.picture.PictureFunctionService;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class GalleryPicture implements PictureInterface
{

    /**
     * Konstruktor ohne recognizedPersons
     */
    public GalleryPicture(String name, String type, byte[] pictureBytes, Person uploader) 
    {
        this.name = name;
        this.type = type;
        this.pictureBytes = pictureBytes;
        this.uploader = uploader;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "type")
    private String type;

    @Enumerated(EnumType.STRING)
    @Column(name = "detection_status")
    private GalleryImage_DetectionStatus status;
    
    @Enumerated(EnumType.STRING)
    private GalleryPictureIdentificationStatus identificationStatus;//Andreas 27.01.2022

    @Lob
    @Column(name = "picture_bytes", columnDefinition = "MEDIUMBLOB")
    private byte[] pictureBytes;

    private Long clientId;

    @Column(name = "upload_date")
    private LocalDate uploadDate;

    @Column(name = "last_edited")
    private LocalDate lastEdited;

    @ManyToOne
    private Person uploader;

    @OneToMany(mappedBy = "gallery_picture", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<GalleryPictureFaceFrame> faceFrames;
    
    
    /**
     * 
     */
    public String showClassification()
    {
        return "";
    }

    /**
     * 
     */
    public String getPictureString()
    {
        return Base64.getEncoder().encodeToString(PictureFunctionService.decompressBytes(this.pictureBytes));
    }

    public Set<GalleryPictureFaceFrame> getGalleryPictureFaceFrame()
    {
        return this.faceFrames;
    }
}
