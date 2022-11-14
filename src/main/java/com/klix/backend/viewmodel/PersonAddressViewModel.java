package com.klix.backend.viewmodel;

import com.klix.backend.model.Address;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
public class PersonAddressViewModel
    {
        public PersonAddressViewModel(PersonAddPersonViewModel p, Address a)            
        {
            id = p.getId();
            firstName = p.getFirstName();
            lastName = p.getLastName();
            email = p.getEmail();
            birthdate = p.getBirthdateString();
            city = a.getCity();
            number = a.getNumber();
            postal_code = a.getPostal_code();
            street = a.getStreet();
        }
        
        public Long id;
        public String firstName;
        public String lastName;
        public String email;
        public String birthdate;
        public String city;
        public String number;
        public String postal_code;
        public String street;
    }
