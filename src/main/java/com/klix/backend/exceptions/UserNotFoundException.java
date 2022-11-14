package com.klix.backend.exceptions;

public class UserNotFoundException extends RuntimeException
{
	private static final long serialVersionUID = 2331896650959629853L;


	/**
     * Construct a UserNotFoundException with the specified detail message.
     */
	public UserNotFoundException(String message) {
		super(message);
	}


	/**
     * Construct a UserNotFoundException with the specified detail message and cause.
     */
	public UserNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}
}