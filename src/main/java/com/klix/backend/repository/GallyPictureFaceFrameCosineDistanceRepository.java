package com.klix.backend.repository;

import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;

import com.klix.backend.model.GalleryPictureFaceFrame;
import com.klix.backend.model.GalleryPictureFaceFrameCosineDistances;

public interface GallyPictureFaceFrameCosineDistanceRepository extends JpaRepository< GalleryPictureFaceFrameCosineDistances, Long> {
    
	Set<GalleryPictureFaceFrameCosineDistances> findAllByFaceFrame (GalleryPictureFaceFrame faceFrame);
}
