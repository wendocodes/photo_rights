package com.klix.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.rest.RepositoryRestMvcAutoConfiguration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;


/**
 * 
 */
@SpringBootApplication
@EnableWebSecurity
@EnableAutoConfiguration(exclude = RepositoryRestMvcAutoConfiguration.class) // Deaktiviert das mapping von Models auf JSON-Endpunkte /[modelname]
public class BackendApplication
{
	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}
}
