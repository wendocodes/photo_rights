package com.klix.backend.exceptions;


/**
 * 
 */
public class StorageException extends RuntimeException {

	
	private static final long serialVersionUID = 2331896650959629853L;


	/**
     * Construct a StorageException with the specified detail message.
     */
	public StorageException(String message) {
		super(message);
	}


	/**
     * Construct a StorageException with the specified detail message and cause.
     */
	public StorageException(String message, Throwable cause) {
		super(message, cause);
	}
}