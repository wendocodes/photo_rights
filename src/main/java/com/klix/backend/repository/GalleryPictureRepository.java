package com.klix.backend.repository;

import java.util.List;
import java.util.Set;

import com.klix.backend.enums.GalleryImage_DetectionStatus;
import com.klix.backend.enums.GalleryPictureIdentificationStatus;
import com.klix.backend.model.GalleryPicture;
import com.klix.backend.model.Person;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GalleryPictureRepository extends JpaRepository<GalleryPicture, Long> {
        static final String INSERT_CLIENT_PERSON_ROLE_QUERY = "INSERT INTO gallery_picture_recognized_person (coordinates, cosine_distance, status, neural_hash, person_id, gallery_picture_id) VALUES";

        public List<GalleryPicture> findByUploader(Person uploader);

        @Query(value = "SELECT * FROM gallery_picture WHERE uploader_id = :uploader_id", nativeQuery = true)
        public List<GalleryPicture> findByUploaderId(@Param("uploader_id") Long personId);

        @Query(value = "SELECT * FROM gallery_picture WHERE client_id = :client_id", nativeQuery = true)
        public List<GalleryPicture> findByClientId(@Param("client_id") Long clientId);

        @Query(value = "SELECT person_id FROM picture_recognized_person WHERE picture_id = :picture_id", nativeQuery = true)
        public Set<Long> findPersonIdsByPictureId(@Param("picture_id") Long pictureId);

        @Query(value = "SELECT * FROM gallery_picture WHERE detection_status = :detection_status", nativeQuery = true)
        public List<GalleryPicture> findByDetectionStatus(@Param("detection_status") String detection_status);

        @Query(value = "SELECT * FROM gallery_picture WHERE identification_status = :identification_status", nativeQuery = true)
        public List<GalleryPicture> findByIdentificationStatus(@Param("identification_status") String identification_status);
}