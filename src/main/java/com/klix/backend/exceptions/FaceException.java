package com.klix.backend.exceptions;

public class FaceException extends RuntimeException
{
	private static final long serialVersionUID = 2331896650959629853L;


	/**
     * Construct a FaceException with the specified detail message.
     */
	public FaceException(String message) {
		super(message);
	}


	/**
     * Construct a FaceException with the specified detail message and cause.
     */
	public FaceException(String message, Throwable cause) {
		super(message, cause);
	}
}