package com.klix.backend.exceptions;

public class TypeException extends RuntimeException
{
	private static final long serialVersionUID = 2331896650959629853L;


	/**
     * Construct a FaceException with the specified detail message.
     */
	public TypeException(String message) {
		super(message);
	}


	/**
     * Construct a FaceException with the specified detail message and cause.
     */
	public TypeException(String message, Throwable cause) {
		super(message, cause);
	}
}