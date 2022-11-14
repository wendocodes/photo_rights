package com.klix.backend.service.user;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityNotFoundException;
import com.klix.backend.exceptions.UserNotFoundException;
import com.klix.backend.model.Role;
import com.klix.backend.model.User;
import com.klix.backend.model.paging.datatable.PagingRequest;
import com.klix.backend.model.paging.datatable.PagingResponse;
import com.klix.backend.repository.RoleRepository;
import com.klix.backend.repository.UserRepository;
import com.klix.backend.repository.projections.IdUsernameAndEmail;
import com.klix.backend.service.interfaces.SecurityService;
import com.klix.backend.validators.groups.ClearPassword;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import lombok.extern.slf4j.Slf4j;


/**
 * Service für userspezifische Methoden
 */
@Service
@Slf4j  // logger
public class UserService
{
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;

    @Autowired protected SecurityService securityService;

    @Autowired private BCryptPasswordEncoder bCryptPasswordEncoder;


    /**
     * Die Methode speichert einen neuen Benutzer.
     * 
     * @param user Das zu speichernde User-Model
     * @return Der gespeicherte User-Datensatz oder null, wenn user == null oder bereits persistent
     */
    public User create(@Validated(ClearPassword.class) User user)
    {
        if (user == null){
            log.info("User not found on save. Exit.");
            return null;
        }

        // user already exists
        if (user.getId() != null && this.findById(user.getId()) != null)
        {
            log.warn("User already exists on create(). Exit.");
            return null;
        }

        // hash password
        String pw = bCryptPasswordEncoder.encode(user.getPassword());
        user.setPassword(pw);
        user.setPasswordConfirm(pw);

        // persist user
        return userRepository.save(user);
    }


    /**
     * Look for a persistent user by id. Then change its direct fields and copy relations to other models. Return the edited instance.
     * 
     * @param user      valid user with changed fields
     * @return          edited and again persisted user, or null if input-user is null or no persistent user was found
     */
    public User edit(@Validated(ClearPassword.class) User user)
    {
        // check input
        if (user == null)
        {
            log.warn("User not found on edit(User). Exit.");
            return null;
        }

        // create new instance
        if (user.getId() == null)
        {
            log.warn("User doesnt exist on edit(User). Exit.");
            return null;
        }

        User existing = userRepository.findById(user.getId()).orElse(null);

        // not jet saved but has id, so just save the new one
        if (existing == null)
        {
            log.warn("Persistent User doesnt exist on edit(User). Exit.");
            return null;
        }

        // save edited
        user.setPerson(existing.getPerson());
        user.setRoles(existing.getRoles());

        // handle password: the one of user is cleartext, the one of existing is hashed

        // if contents match, copy the existing
        if (bCryptPasswordEncoder.matches(user.getPassword(), existing.getPassword()))
        {
            user.setPassword(existing.getPassword());
            user.setPasswordConfirm(existing.getPassword());

        // else: pw has changed, hash cleartext password and use it
        } else {
            String pw = bCryptPasswordEncoder.encode(user.getPassword());
            user.setPassword(pw);
            user.setPasswordConfirm(pw);
        } 
        
        return userRepository.save(user);
    }

    
    /**
     * Looks for a persistent user by id, changes its username and email and returns edited instance
     * 
     * @param id        unique user_id of the user (is a lowercase long to prevent a value of null)
     * @param username  new username
     * @param email     new email
     * @return          edited and again persisted user, or null if no persistent user was found
     */
    public User edit(long id, String username, String email)
    {
        User newUser = new User(username, null, email);
        newUser.setId(id);

        User existing = userRepository.findById(id).orElse(null);

        // not jet saved, so just save the new one
        if (existing == null)
        {
            log.warn("Persistent User doesnt exist on edit(id, username, email). Exit.");
            return null;
        }

        // copy all other fields & all relations
        newUser.setPassword(existing.getPassword());
        newUser.setPasswordConfirm(existing.getPassword());

        newUser.setPerson(existing.getPerson());
        newUser.setRoles(existing.getRoles());

        return userRepository.save(newUser);
    }

    
    /**
     * Find user by username
     * 
     */
    public User findByUsername(String username){
        return userRepository.findByUsername(username).orElse(null);
    }


    /**
     * Validate user against username and password
     * @return user data
     */
	public User validateUser(String username, String password) {

        // get user by username
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null)
        {
            return user;
        }

        // if found, compare passwords
        String passwordDb = user.getPassword();
        log.info("password " + bCryptPasswordEncoder.encode(password));
        log.info("passwordDb " + passwordDb);

        return bCryptPasswordEncoder.matches(password, passwordDb) ? user : null;
	}

     /**
     *  find a user by email
     */
    public User findUserByEmail(String email){
        return userRepository.findByEmail(email).orElse(null);
    }

    
    /**
     *  finds a user by the given reset password token.
     */
    public User findByToken(String token){
        return userRepository.findByResetPasswordToken(token).orElse(null);
    }   

    /**
     * set value for resetPasswordToken of a user found by email, persist change to the database. 
     * @throws UserNotFoundException if user not found
     */
    public void updateResetPasswordToken(String token, String email) throws UserNotFoundException {
        User user = userRepository.findByEmail(email).orElse(null);
      
            try{
                user.setResetPasswordToken(token);
                userRepository.save(user);
            } catch(UserNotFoundException e) {
                log.info("Could not find any user with the email" + email);
            } 
    }

     /**
     * sets new password for the user (using BCrypt password encoding) 
     * nullifies the reset password token.
     */
    public void updatePassword(User user, String newPassword) {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String encodedPassword = passwordEncoder.encode(newPassword);
        user.setPassword(encodedPassword);
         
        user.setResetPasswordToken(null);
        userRepository.save(user);
    }


    /**
     * 
     */
    public User findByPersonId(Long personId){
        return userRepository.findByPersonId(personId).orElse(null);
    }

    
    /**
     * 
     */
    public User addRole(User user, String roleName){
        Role userRole = roleRepository.findByName(roleName).orElse(null);

        if (userRole == null){
            userRole = new Role(roleName);
        }

        return this.addRole(user, userRole);
    }


    /**
     * Add the role to the user if they are both not null
     * 
     * @param user  user instance that recieves a role
     * @param role  role it recieves
     * @return  user instance with new (and persisted) role, or the unchanged user if either param has been null
     */
    public User addRole(User user, Role role)
    {
        if (user == null)
        {
            log.warn(String.format("User param is null in addRole with role: %s. Exit.", role.toString()));
            return null;
        }

        if (role == null)
        {
            log.warn(String.format("Role param is null in addRole with user: %s. Exit.", user.toString()));
            return user;
        }

        // collect all roles of user
        Set<Role> roles = user.getRoles();

        // Null Check
        if (roles == null){
            roles = new HashSet<>();
        }

        roles.add(role);

        // persist them
        roleRepository.saveAll(roles);
        user.setRoles(roles);

        // return user with new roles
        return user;
    }

    
    /**
     * 
     */
    public List<IdUsernameAndEmail> getAllIdUsernameAndEmail(){
        return userRepository.getAllIdUsernameAndEmail();
    }


    /**
     * 
     */
    public User getOne(long id) throws EntityNotFoundException
    {
        return userRepository.getOne(id);
    }

    
    /**
     * 
     */
    public User findById(long id){
        return userRepository.findById(id).orElse(null);
    }


    /**
     * Die Methode löscht einen User und seine Rollen mittels Id
     * 
     * @param id Die Id des Users
     */
    public void deleteById(long id){
        User user = userRepository.findById(id).orElse(null);

        if (user != null)
        {
            // Entfernen der Rollen
            for (Role role : user.getRoles()){
                user.removeRole(role);
            }
            
            // Entfernen des Users
            userRepository.deleteById(id);
        }
    }


    /**
     * 
     */
    public void deleteInBatch(Iterable<User> instances){
        userRepository.deleteInBatch(instances);
    }


    /**
     * 
     */
    public PagingResponse<IdUsernameAndEmail> getPage(PagingRequest request)
    {
        Pageable pageable = PageRequest.of(request.getPageIndex(), request.getLength(), request.getSort());
        Page<IdUsernameAndEmail> page = userRepository.findPage(pageable);

        /**
         * !TODO: update second page.getTotalElements() to something for Sort
         */
        return new PagingResponse<>(request.getDraw(), page.getTotalElements(), page.getTotalElements(), page.getContent());
    }
}