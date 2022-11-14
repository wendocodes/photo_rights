package com.klix.backend.viewmodel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.klix.backend.enums.PublicationResponseStatus;
import com.klix.backend.model.Client;
import com.klix.backend.model.GalleryPicture;
import com.klix.backend.model.Person;
import com.klix.backend.model.PublicationRequest;
import com.klix.backend.model.PublicationResponse;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MitarbeiterGalleryViewModel {
    private Client client;
    private List<UploaderSection> uploaderSections = new ArrayList<>(); // use List insted of Set to keep the ordering

    @Getter
    @Setter
    @AllArgsConstructor
    public class Uploader {
        private Person person;
        private Set<String> role_names;

        /**
         * Tests only by uploader.getPerson()
         */
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Uploader))
                return false;

            Uploader u = (Uploader) o;
            return this.person.equals(u.getPerson());
        }

        /**
         * Add specified roleName to role_names
         * 
         * @return true if successfully inserted, false if already existed or roleName
         *         was null.
         */
        public boolean addRoleName(String roleName) {
            return roleName != null ? this.role_names.add(roleName) : null;
        }

        public String getRoles() {
            return String.join(", ", this.role_names);
        }
    }

    @Getter
    @Setter
    public class UploaderSection {
        private Uploader uploader;
        private List<Image> images = new ArrayList<>(); // use List insted of Set to keep the ordering

        /**
         * Adds given image to images.
         * 
         * @return true if insertion was successful, false if already exists or if omage
         *         was null.
         */
        public boolean addImage(Image image) {
            if (image == null || this.images.contains(image))
                return false;

            this.images.add(image);
            return true;
        }

        public Long getUploaderPersonId() {
            return this.getUploader().getPerson().getId();
        }
    }

    @Getter
    @Setter
    public class Image {
        private GalleryPicture picture;
        private List<Person> recognizedPersons;

        private PublicationRequest pubRequest;
        private Set<PublicationResponse> pubResponses;
        private Map<String, List<String>> personNameByResponseStatus = new HashMap<>();

        /**
         * Tests only by getPicture()
         */
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Image))
                return false;

            Image i = (Image) o;
            return this.picture.equals(i.getPicture());
        }

        public Set<PublicationResponse> getPubResponsesOfStatus(PublicationResponseStatus status) {
            return this.pubResponses != null ? this.pubResponses.stream()
                    .filter(pubResponse -> pubResponse.getStatus() == status)
                    .collect(Collectors.toSet())
                    : new HashSet<>();
        }

        public Map<String, Integer> getPubStatistic() {
            Map<String, Integer> pubStatistic = new HashMap<>();
            pubStatistic.put("allowed", this.getPubResponsesOfStatus(PublicationResponseStatus.ALLOWED).size());
            pubStatistic.put("forbidden", this.getPubResponsesOfStatus(PublicationResponseStatus.FORBIDDEN).size());
            pubStatistic.put("pending", this.getPubResponsesOfStatus(PublicationResponseStatus.PENDING).size());
            pubStatistic.put("not_asked", this.getRecognizedPersons().size() - pubStatistic.get("allowed")
                    - pubStatistic.get("forbidden") - pubStatistic.get("pending"));
            pubStatistic.put("unrecognized",
                    this.getPicture().getFaceFrames().size() - this.getRecognizedPersons().size());

            pubStatistic.put("count", this.getPicture().getFaceFrames().size());
            return pubStatistic;
        }
    }

    /**
     * Adds an UploaderSection with given uploader to uploaderSections, if one with
     * given uploader did not exist jet.
     * Returns that UploaderSection or the already exixsting one of uploader
     * 
     * @param uploader the uploader to look for. If null do nothing and return null
     * @return the newly created or retrieved UploaderSection of specified uploader,
     *         null if uploader was null.
     */
    public UploaderSection addUploader(Uploader uploader) {
        if (uploader == null)
            return null;

        // construct new UploaderSection with given Uploader
        UploaderSection currentSection = new UploaderSection();
        currentSection.setUploader(uploader);

        // compare UploaderSections by custom equals(). Returns true if UploaderSection
        // with uploader didn't exist jet
        boolean alreadyExists = !this.uploaderSections.add(currentSection);
        if (!alreadyExists)
            return currentSection;

        return this.getUploaderSection(uploader);
    }

    /**
     * Get an UploaderSection by its uploader
     * 
     * @return the retrieved UploaderSection of set user or null if not existing.
     */
    public UploaderSection getUploaderSection(Uploader uploader) {
        for (UploaderSection section : this.uploaderSections) {
            if (uploader.equals(section.getUploader()))
                return section;
        }
        return null;
    }
}