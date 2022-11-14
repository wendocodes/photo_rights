package com.klix.backend.model.app;

import com.klix.backend.model.Person;
import lombok.Getter;
import lombok.Setter;


/**
 * Request data of /register
 */
@Getter
@Setter
public class PersonAndUserCredentials {
    Person person;
    UserCredentials user;
}