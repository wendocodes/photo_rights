package com.klix.backend.controller.app;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolation;

import com.klix.backend.constants.RoleConstants;
import com.klix.backend.controller.BaseController;
import com.klix.backend.exceptions.UserNotFoundException;
import com.klix.backend.enums.RegistrationStatus;
import com.klix.backend.model.Person;
import com.klix.backend.model.User;
import com.klix.backend.model.app.PersonAndUserCredentials;
import com.klix.backend.model.app.PinRequest;
import com.klix.backend.model.app.UserCredentials;
import com.klix.backend.model.interfaces.Utility;
import com.klix.backend.repository.UserRepository;
import com.klix.backend.repository.PersonRepository;
import com.klix.backend.service.app.JwtUtilsService;
import com.klix.backend.validators.groups.ClearPassword;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.utility.RandomString;


/**
 * TODO: Kommentar
 */
@Slf4j
@RestController
@RequestMapping(path = "/app/api/v01")
@CrossOrigin(origins = "*")
// Diese Einstellung sollte in Produktion verwendet werden:
// @CrossOrigin(origins = "http://localhost:8100")
public class AppApiController extends BaseController
{
	@Autowired private JwtUtilsService jwtUtilsService;
	@Autowired private PersonRepository personRepository;

	@Autowired private UserRepository userRepository;

	@Autowired private LocalValidatorFactoryBean validator;


	/**
	 * Use a response to check some conditions: [Wird aktuell nicht verwendet]
	 */
	@GetMapping("/userinfo")
	public ResponseEntity<String> userinfo(final HttpServletRequest request, final HttpServletResponse response) {
		// Check if the request has any exception that we have stored in Request
		final Exception exception = (Exception) request.getAttribute("exception");

		if (jwtUtilsService.checkRequest(request, response) || exception == null)
		{
			return ResponseEntity.ok("You are authenticated!");
		} else
		{
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		}
	}

	/**
	 * Provide user credentials to receive a Token:
	 * 
	 * @return a response with an ok if user credentials
	 */
	@PostMapping("/login")
	public ResponseEntity<UserCredentials> login(@RequestBody UserCredentials userCredentials)
	{
		// get fields
		final String username = userCredentials.getUsername().trim();
		final String password = userCredentials.getPwd().trim();

		// ckeck if valid user
		User user = userService.validateUser(username, password);
		if (user == null)
		{
			log.info("user with username " + username + " not valid.");
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		}

		// generate token
		final String token = jwtUtilsService.generateJWTToken(username);
		log.debug("in login(): generated token: " + token);

		// send token back
		userCredentials.setPwd(null);
		userCredentials.setToken(token);

		return ResponseEntity.ok(userCredentials);
	}

	/**
	 * Provide forgot password token to reset the password
	 * 
	 * @return a response with a reset password token if user credentials
	 */
	@PostMapping("/resetPassword")
	public ResponseEntity<UserCredentials> passwordReset(@RequestBody UserCredentials data, final HttpServletRequest request, final HttpServletResponse responseHttpServletResponse)
	{	
		String email = data.getEmail().trim();
		
		// check if user with given email already exists
		User user = userRepository.findByEmail(email).orElse(null);
	
        String token = RandomString.make(30);

		try{
			if (user != null)
			{	
				userService.updateResetPasswordToken(token, email);
				String resetPasswordLink = Utility.getSiteURL(request) + "/reset_password?token=" + token;
				log.info("link"+ resetPasswordLink);
				emailService.sendPasswordResetMail(email, resetPasswordLink);

				return ResponseEntity.ok(data);
			
			}

		return new ResponseEntity<UserCredentials>(HttpStatus.FORBIDDEN);	

		} catch( UserNotFoundException noUser){
            String message = messageSource.getMessage("root.login.reset.emailConstraints", null, null);	
			log.info("catch user not found" +message );
			return new ResponseEntity<UserCredentials>(HttpStatus.FORBIDDEN);
		}
		
	}

	/**
	 * Provide user credentials and person to register a user and generate his/her token
	 * ! Ignore this endpoint it's not currently in use, because legal guardians are not registering themselves in the app
	 * ! Just keeping the code since we might need it later
	 * */
	@PostMapping("/registration")
	public ResponseEntity<?> register(@RequestBody PersonAndUserCredentials data)
	{

		UserCredentials userCredentials = data.getUser();
		
		// 1. FETCHING USER BY EMAIL AND USERNAME
		final String username = userCredentials.getUsername();
		User user = userService.findByUsername(username);

		User userByEmail = userService.findUserByEmail(userCredentials.getEmail());

		//2. IF USER IS NOT FOUND, SENDING BAD REQUEST RESP
		if (user != null) 
			return ResponseEntity.badRequest().body("Username already used");

		if(userByEmail != null) 
			return ResponseEntity.badRequest().body("email already used. Check email ");
		

		//3. 
		Person person = personRepository.findByEmail(userCredentials.getEmail()).orElse(null);
		if(person == null) {
			// No person found with that email
			return ResponseEntity.badRequest().body("Please register on web for credentials!");
		} else {
			person.setFirstName(Optional.of(data.getPerson().getFirstName()).orElse(person.getFirstName()));
			person.setLastName(Optional.of(data.getPerson().getLastName()).orElse(person.getLastName()));
			person.setBirthdate(Optional.of(data.getPerson().getBirthdate()).orElse(person.getBirthdate()));
			person = personService.save(person);
		}
		// convert userCredentials into user
		user = new User(userCredentials.getUsername(), userCredentials.getPwd(), userCredentials.getEmail());
		user.setPasswordConfirm(user.getPassword());

		// validate user instance
		Map<String, Set<String>> validation_errors = new HashMap<>();
		Set<ConstraintViolation<User>> violations_user = validator.validate(user, ClearPassword.class);
		if (!violations_user.isEmpty())
		{
			for (ConstraintViolation<User> violation : violations_user)
			{
				Set<String> err = validation_errors.getOrDefault(violation.getPropertyPath().toString(), new HashSet<>());
				err.add(violation.getMessage());
				validation_errors.put(violation.getPropertyPath().toString(), err);
			}
		}

		Set<ConstraintViolation<Person>> violations_person = validator.validate(person);
		if (!violations_person.isEmpty())
		{
			for (ConstraintViolation<Person> violation : violations_person)
			{
				Set<String> err = validation_errors.getOrDefault(violation.getPropertyPath().toString(), new HashSet<>());
				err.add(violation.getMessage());
				validation_errors.put(violation.getPropertyPath().toString(), err);
			}
		}

		if (!validation_errors.isEmpty())
		{
			log.warn("in app-registration: " + validation_errors.toString());
			return new ResponseEntity<Map<String, Set<String>>>(validation_errors, HttpStatus.UNPROCESSABLE_ENTITY);
		}

		user.setPerson(person);

		User roleUser = userService.addRole(user, RoleConstants.user);
		if (roleUser == null)
		{
			// adding a role went wrong
			personService.deleteById(person.getId());
			return new ResponseEntity<UserCredentials>(HttpStatus.UNPROCESSABLE_ENTITY);
		}

		// save user and its roles to db
		user = userService.create(user);
		if (user == null)
		{
			// creating a user went wrong
			personService.deleteById(person.getId());
			return new ResponseEntity<UserCredentials>(HttpStatus.UNPROCESSABLE_ENTITY);
		}

		// generate token
		final String token = jwtUtilsService.generateJWTToken(username);

		// send back
		userCredentials.setPwd(null);
		userCredentials.setToken(token);

		return ResponseEntity.ok(userCredentials);
	}

	/**
	 * Create login credentials after PIN verification
	 * TODO: Better comments here
	 * @param {}
	 * @return
	 */

	@PostMapping("/create_credential")
	public ResponseEntity<?> credentials(@RequestBody PersonAndUserCredentials data)
	{
	
		UserCredentials userCredentials = data.getUser();
		
		// 1. FETCHING USER BY EMAIL AND USERNAME
		final String username = userCredentials.getUsername();
		User user = userService.findByUsername(username);

		User userByEmail = userService.findUserByEmail(userCredentials.getEmail());

		//2. IF USER IS NOT FOUND, SENDING BAD REQUEST RESP
		if (user != null) 
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);  

		if(userByEmail != null) 
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);  
		

		//3. FETCH PERSON BY EMAIL AND UPDATE THE FIELDS
		Person person = personRepository.findByEmail(userCredentials.getEmail()).orElse(null);
		if(person == null) {
			return ResponseEntity.badRequest().body("Please register on web for credentials!");
		} else {
			person.setFirstName(Optional.of(data.getPerson().getFirstName()).orElse(person.getFirstName()));
			person.setLastName(Optional.of(data.getPerson().getLastName()).orElse(person.getLastName()));
			person.setBirthdate(Optional.of(data.getPerson().getBirthdate()).orElse(person.getBirthdate()));
			person = personService.save(person);
		}

		// convert userCredentials into user
		user = new User(userCredentials.getUsername(), userCredentials.getPwd(), userCredentials.getEmail());
		user.setPasswordConfirm(user.getPassword());

		// validate user instance
		Map<String, Set<String>> validation_errors = new HashMap<>();
		Set<ConstraintViolation<User>> violations_user = validator.validate(user, ClearPassword.class);
		if (!violations_user.isEmpty())
		{
			for (ConstraintViolation<User> violation : violations_user)
			{
				Set<String> err = validation_errors.getOrDefault(violation.getPropertyPath().toString(), new HashSet<>());
				err.add(violation.getMessage());
				validation_errors.put(violation.getPropertyPath().toString(), err);
			}
		}

		user.setPerson(person);

		User roleUser = userService.addRole(user, RoleConstants.user);
		if (roleUser == null)
		{
			// adding a role went wrong
			personService.deleteById(person.getId());
			return new ResponseEntity<UserCredentials>(HttpStatus.UNPROCESSABLE_ENTITY);
		}

		// save user and its roles to db
		user = userService.create(user);
		if (user == null)
		{
			// creating a user went wrong
			personService.deleteById(person.getId());
			return new ResponseEntity<UserCredentials>(HttpStatus.UNPROCESSABLE_ENTITY);
		}

		// generate token
		final String token = jwtUtilsService.generateJWTToken(username);

		// send back
		userCredentials.setPwd(null);
		userCredentials.setToken(token);

		return ResponseEntity.ok(userCredentials);
	}

	/**
	 * Login to a Kindergarten using a PIN
	 * @param pin
	 * @return a response with an ok if PIN matches
	 */
	@PostMapping("/verifyPin")
	public ResponseEntity<?> verifyPin(@RequestBody PinRequest pin)
	{

		if(pin==null || pin.getPin()==null){
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		// find Legal guardian by pin
		Person person = personRepository.findByPin(pin.getPin());
		
		if(person != null)
        {  
            person.setStatus(RegistrationStatus.APPROVED);
			person.setPin(null);
			personRepository.save(person);
			
			return new ResponseEntity<>(person, HttpStatus.OK);
           
        } else{
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);  
		}	
	} 
}