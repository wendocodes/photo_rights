package com.klix.backend.exceptions;

public class GalleryPictureNotFoundException extends RuntimeException {
    /**
     * Construct a StorageException with the specified detail message and cause.
     */
	public GalleryPictureNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

    public GalleryPictureNotFoundException() {
    }
}
