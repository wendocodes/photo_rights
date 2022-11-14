package com.klix.backend.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;

import java.io.IOException;
import java.util.Base64;

import com.klix.backend.model.interfaces.PictureInterface;
import com.klix.backend.service.picture.PictureFunctionService;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

/**
 * 
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
public class IdPicture implements PictureInterface
{
    /**
     * Der Konstruktor ist obsolet, Person soll "not nullable" werden.
     */
    public IdPicture(String name, String type, byte[] pictureBytes) {
        this.name = name;
        this.type = type;
        this.pictureBytes = pictureBytes;
        //this.person = person; //sollte irgendwann not null werden
    }

    /**
     * Konstruktor mit Person:
     */
    public IdPicture(String name, String type, byte[] pictureBytes, Person person) {
        this.name = name;
        this.type = type;
        this.pictureBytes = pictureBytes;
        this.person = person; //sollte irgendwann not null werden
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    private String type;

    @Column(name = "name")
    private String name;

    @Lob
    @Column(columnDefinition = "MEDIUMBLOB")
    private byte[] pictureBytes;

    @Lob
    @Column(columnDefinition = "MEDIUMBLOB")
    private byte[] kiPictureBytes;

    @ManyToOne
    //@NotNull  // sollte bald not null werden weil Referenzbilder immer zu einer Person gehören
    private Person person;

    @Lob
    @Column(columnDefinition = "MEDIUMBLOB")
    private String neuralHash;


    /**
     * 
     */
    public String showClassification()
    throws IOException {
        return "";
    }


    /**
     * 
     */
    public String getPictureString() {
        return Base64.getEncoder().encodeToString(PictureFunctionService.decompressBytes(this.pictureBytes));
    }

    /**
     * PicturePersistence uses this method to decompress the image which is used by the Ai.
     */
    public String getKiPictureString() {
        return Base64.getEncoder().encodeToString(PictureFunctionService.decompressBytes(this.kiPictureBytes));
    }
}