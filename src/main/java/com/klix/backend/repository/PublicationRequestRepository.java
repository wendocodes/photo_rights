package com.klix.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Set;

import com.klix.backend.model.PublicationRequest;

public interface PublicationRequestRepository extends JpaRepository<PublicationRequest, Long>{

    public Set<PublicationRequest> findByClientIdAndGalleryPictureId(Long clientId, Long imageId);
    
    public Set<PublicationRequest> findByGalleryPictureId(Long imageId);
}