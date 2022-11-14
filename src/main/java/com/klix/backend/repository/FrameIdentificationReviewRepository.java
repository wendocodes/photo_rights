package com.klix.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.klix.backend.model.FrameIdentificationReview;

public interface FrameIdentificationReviewRepository extends JpaRepository<FrameIdentificationReview, Long> {

	List<FrameIdentificationReview> findAllByFrameID(Long faceFrameId);

}
