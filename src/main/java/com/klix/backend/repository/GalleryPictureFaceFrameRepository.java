package com.klix.backend.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

import com.klix.backend.model.GalleryPictureFaceFrame;

public interface GalleryPictureFaceFrameRepository extends JpaRepository<GalleryPictureFaceFrame, Long> {

    @Query(value = "SELECT * FROM gallery_picture_face_frame WHERE gallery_picture_id = :gallery_picture_id" , nativeQuery = true)
    public List<GalleryPictureFaceFrame> findByGalleryPictureId(@Param("gallery_picture_id") Long galleryPictureId);

    @Query(value = "SELECT * FROM gallery_picture_face_frame WHERE identification_status = :identification_status && gallery_picture_id = :gallery_picture_id ", nativeQuery = true)
        public List<GalleryPictureFaceFrame> findByIdentificationStatus (
                        @Param("identification_status") int identification_status, long gallery_picture_id);

} 

