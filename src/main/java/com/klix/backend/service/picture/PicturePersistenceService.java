package com.klix.backend.service.picture;

import com.klix.backend.enums.GalleryImage_DetectionStatus;
import com.klix.backend.exceptions.GalleryPictureNotFoundException;
import com.klix.backend.exceptions.StorageException;
import com.klix.backend.model.GalleryPicture;
import com.klix.backend.model.GalleryPictureFaceFrame;
import com.klix.backend.model.interfaces.PictureInterface;
import com.klix.backend.repository.GalleryPictureRepository;
import com.klix.backend.repository.GalleryPictureFaceFrameRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 */
@Service
@Slf4j
public class PicturePersistenceService {
    @Autowired
    private GalleryPictureRepository galleryPictureRepository;
    @Autowired
    private GalleryPictureFaceFrameRepository galleryPictureFaceFrameRepository;

    public List<Integer[]> getFaceFrameCoordinates(long galleryImageId) {
        List<Integer[]> coordinates = new ArrayList<>();
        List<GalleryPictureFaceFrame> galleryImageFaceFrames = galleryPictureFaceFrameRepository
                .findByGalleryPictureId(galleryImageId);
        if (!galleryImageFaceFrames.isEmpty()) {
            for (GalleryPictureFaceFrame faceFrame : galleryImageFaceFrames) {
                coordinates.add(faceFrame.getCoordinates());
            }
        }
        return coordinates;
    }

    /**
     * Löschen eines Bildes
     */

    public void delete(long id, CrudRepository<? extends PictureInterface, Long> repo) throws StorageException {
        if (id <= 0) {
            throw new StorageException("Delete failed, there was no id given.");
        }
        repo.deleteById(id);
    }

    public void deleteCoordinates(long galleryImageId, Integer[] markedCoordinates) {
        List<GalleryPictureFaceFrame> galleryPictureFaceFrameList = galleryPictureFaceFrameRepository
                .findByGalleryPictureId(galleryImageId);
        for (GalleryPictureFaceFrame galleryPictureFaceFrame : galleryPictureFaceFrameList) {
            Integer[] coordinates = galleryPictureFaceFrame.getCoordinates();
            if (Arrays.equals(coordinates, markedCoordinates)) {
                galleryPictureFaceFrameRepository.delete(galleryPictureFaceFrame);
            }
        }
    }

    public void saveGalleryImageStatus(long galleryImageId, GalleryImage_DetectionStatus status)
            throws GalleryPictureNotFoundException {
        GalleryPicture galleryImage = galleryPictureRepository.findById(galleryImageId).orElse(null);
        if (galleryImage == null) {
            throw new GalleryPictureNotFoundException();
        }

        log.info(status.toString());
        galleryImage.setStatus(status);
        galleryPictureRepository.save(galleryImage);
    }
}
