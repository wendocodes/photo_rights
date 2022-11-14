package com.klix.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Set;

import com.klix.backend.enums.PublicationResponseStatus;
import com.klix.backend.model.PublicationResponse;

public interface PublicationResponseRepository extends JpaRepository<PublicationResponse, Long> {
    public Set<PublicationResponse> findByPublicationRequestId(Long requestId);

    public Set<PublicationResponse> findByPersonId(Long personId);

    public Set<PublicationResponse> findByStatusAndPersonId(PublicationResponseStatus status, Long personId);

    public Set<PublicationResponse> findByPersonIdOrderByPublicationRequestIdDesc(Long requestId);
}