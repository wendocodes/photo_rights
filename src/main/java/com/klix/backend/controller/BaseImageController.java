package com.klix.backend.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.klix.backend.enums.GalleryPictureFaceFrameIdentificationStatus;
import com.klix.backend.exceptions.GalleryPictureNotFoundException;
import com.klix.backend.model.FrameIdentificationReview;
import com.klix.backend.model.GalleryPicture;
import com.klix.backend.model.GalleryPictureFaceFrame;
import com.klix.backend.repository.AddressRepository;
import com.klix.backend.repository.FrameIdentificationReviewRepository;
import com.klix.backend.repository.GalleryPictureFaceFrameRepository;
import com.klix.backend.repository.GroupsRepository;
import com.klix.backend.repository.IdPictureRepository;
import com.klix.backend.repository.PersonRepository;
import com.klix.backend.repository.PublicationRequestRepository;
import com.klix.backend.repository.PublicationResponseRepository;
import com.klix.backend.repository.UserRepository;
import com.klix.backend.service.GroupsService;
import com.klix.backend.service.picture.PictureFunctionService;
import com.klix.backend.service.picture.PicturePersistenceService;
import com.klix.backend.service.picture.PictureUploadService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;

public class BaseImageController extends BaseController {

    @Autowired
    protected PicturePersistenceService picturePersistenceService;

    @Autowired
    protected PictureFunctionService pictureFunctionService;

    @Autowired
    protected PictureUploadService pictureUploadService;

    @Autowired
    protected GroupsService groupsService;

    @Autowired
    protected IdPictureRepository idPictureRepository;

    @Autowired
    protected PersonRepository personRepository;

    @Autowired
    protected FrameIdentificationReviewRepository frameIdentificationReviewRepository;

    @Autowired
    protected PublicationRequestRepository publicationRequestRepository;

    @Autowired
    protected GalleryPictureFaceFrameRepository galleryPictureFaceFrameRepository;

    @Autowired
    protected PublicationResponseRepository publicationResponseRepository;

    @Autowired
    protected GroupsRepository groupsRepository;

    @Autowired
    protected AddressRepository addressRepository;

    @Autowired
    protected UserRepository userRepository;

    public boolean checkMultipartFile(MultipartFile file) throws IOException {
        final int MAX_FILE_SIZE = 12000000;

        // if file exceeds 12 Mb, it cannot be uploaded.
        return !file.isEmpty() && file.getBytes().length <= MAX_FILE_SIZE;
    }

    public boolean checkIdPictureIdOfPerson(long personId) {
        return idPictureRepository.findByPersonId(personId).orElse(null) != null;
    }

    public boolean checkIdPictureId(long idPictureId) {
        return idPictureRepository.findById(idPictureId).orElse(null) != null;
    }

    /**
     * helper function to get all recognizedPersonIds from an GalleryImage with
     * status KI_SUGGESTION_ACCEPTED or SELF_SEARCHED
     */
    public List<Long> computeRecognizedPersonsIds(Long galleryImageId) throws GalleryPictureNotFoundException {

        // instantiate responses for all recognized_persons
        GalleryPicture galleryPicture = this.galleryPictureRepository.findById(galleryImageId).orElse(null);
        if (galleryPicture == null) {
            throw new GalleryPictureNotFoundException();
        }
        Set<GalleryPictureFaceFrame> faceFrames = galleryPicture.getFaceFrames();
        List<Long> recognizedPersonIds = new ArrayList<>();

        for (GalleryPictureFaceFrame faceFrame : faceFrames) {
            if (faceFrame.getIdentificationStatus()
                    .equals(GalleryPictureFaceFrameIdentificationStatus.KI_SUGGESTION_ACCEPTED) ||
                    faceFrame.getIdentificationStatus()
                            .equals(GalleryPictureFaceFrameIdentificationStatus.SELF_SEARCHED)) {
                List<FrameIdentificationReview> reviews = frameIdentificationReviewRepository
                        .findAllByFrameID(faceFrame.getFaceFrameId());
                if (reviews != null && !reviews.isEmpty()) {
                    Collections.sort(reviews);
                    FrameIdentificationReview latestReview = reviews.get(reviews.size() - 1);
                    recognizedPersonIds.add(latestReview.getPersonID());
                }
            }
        }
        return recognizedPersonIds;
    }

    public boolean checkImageType(MultipartFile file) {
        final String[] ALLOWED_EXTENSIONS = { "image/png", "image/jpg", "image/jpeg" };
        final String extension = file.getContentType().toLowerCase();

        return Arrays.asList(ALLOWED_EXTENSIONS).contains(extension);
    }
}
