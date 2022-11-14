package com.klix.backend.model;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import com.klix.backend.enums.PublicationRequestStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class PublicationRequest {
    public PublicationRequest() {
        this.createdAt = new Date();
    }

    public PublicationRequest(Long clientId, Long galleryPictureId, String text) {
        this();
        this.clientId = clientId;
        this.galleryPictureId = galleryPictureId;
        this.text = text;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String text;

    private Date createdAt;

    private Long galleryPictureId;

    private Long clientId;
    @Enumerated(EnumType.STRING)
    private PublicationRequestStatus status = PublicationRequestStatus.REQUESTED;
}