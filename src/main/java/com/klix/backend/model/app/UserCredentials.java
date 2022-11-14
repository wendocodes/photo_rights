package com.klix.backend.model.app;

import lombok.Getter;
import lombok.Setter;



/**
 * UserCredentials
 */
@Getter
@Setter
public class UserCredentials
{
    private String username;
	private String pwd;
	private String email;
	private String token;
}
