package com.klix.backend.controller;

import com.klix.backend.constants.RoleConstants;
import com.klix.backend.model.Person;
import com.klix.backend.model.User;
import com.klix.backend.repository.PersonRepository;
import com.klix.backend.repository.projections.IdUsernameAndEmail;
import com.klix.backend.service.user.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;


/**
 * 
 */
@Controller
@RequestMapping(path="/api")
public class ApiController
{
	@Autowired
	private PersonRepository personRepository;

    @Autowired
    private UserService userService;


	/**
     * 
     */
	@PostMapping(path="/add")
	public @ResponseBody String addNewUser (@RequestParam String username,
											@RequestParam String password,
											@RequestParam String email)
	{
        User user = new User(username, password, email);
        userService.addRole(user, RoleConstants.user);
        userService.create(user);

		return "Saved";
	}


	/**
     * 
     */
	@GetMapping(path="/allusernames")
	public @ResponseBody Iterable<IdUsernameAndEmail> getAllUsernames()
	{
		return userService.getAllIdUsernameAndEmail();
	}


	/**
     * 
     */
	@GetMapping(path="/allpersons")
	public @ResponseBody Iterable<Person> getAllPersons()
	{
		return personRepository.findAll();
	}
	
}