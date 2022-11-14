package com.klix.backend.service;

import java.util.List;
import java.util.Set;

import javax.persistence.EntityNotFoundException;

import com.klix.backend.model.Client;
import com.klix.backend.model.ClientRole;
import com.klix.backend.model.Person;
import com.klix.backend.model.User;
import com.klix.backend.model.paging.datatable.PagingRequest;
import com.klix.backend.model.paging.datatable.PagingResponse;
import com.klix.backend.repository.IdPictureRepository;
import com.klix.backend.repository.PermissionRepository;
import com.klix.backend.repository.ClientRepository;
import com.klix.backend.repository.ClientRoleRepository;
import com.klix.backend.repository.PersonRepository;
import com.klix.backend.repository.UserRepository;
import com.klix.backend.repository.projections.PersonPermission;
import com.klix.backend.service.interfaces.SecurityService;
import com.klix.backend.service.user.UserService;
import com.klix.backend.enums.RegistrationStatus;
import java.util.concurrent.ThreadLocalRandom;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.time.LocalDate;

import lombok.extern.slf4j.Slf4j;


/**
 * 
 */
@Service
@Slf4j  // logger
public class PersonService
{
    @Autowired private PersonRepository personRepository;
    @Autowired private IdPictureRepository idPictureRepository;
    @Autowired private ClientRepository clientRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PermissionRepository permissionRepository;

    @Autowired private PermissionService permissionService;
    @Autowired protected SecurityService securityService;
    @Autowired protected UserService userService;
    @Autowired private EmailService emailService;
    @Autowired private ClientRoleRepository clientRoleRepository;

    /**
     * Create legal guardian person
     */
    public Person createParent(String firstName, String lastName, String email, Long clientId) {

        Person guardian = findByEmailAndClientId(email, clientId);

        if(guardian == null) {

            Integer pin= genPin();
            guardian= new Person(firstName, lastName, email, pin, RegistrationStatus.PENDING_APPROVAL);
            guardian = personRepository.save(guardian);
            assignRoleAndPermissionToPerson(clientId, guardian.getId(), "Benutzer");
            emailService.sendmailToParent(email, pin);
        }
        return guardian;
    }

    /**
     * Utility Method that autogenerates the PIN for parents
     */
    public int genPin() {
        // generates a number value btn starting and ending value
        int pin = ThreadLocalRandom.current().nextInt(10000, 100000 + 1);
   
        return pin;  
    }

    /**
     * Assign a role to person
     */
    private void assignRoleAndPermissionToPerson(Long clientId, Long personId, String role) {

        permissionRepository.insertClientPerson(clientId, personId);

        Long parentClientPersonId = permissionRepository.findByClientPersonId(clientId, personId);

        ClientRole clienRole =

                clientRoleRepository.findByName(role)

                .orElseThrow(() -> new RuntimeException("Role not found"));

        permissionRepository.insertClientPersonRole(parentClientPersonId, clienRole.getId());

    }


    /**
     * create child person
     */
    public Person createChild(String firstName, String lastName, LocalDate dateOfBirth, Long clientId) 
    {

        Person child = new Person(firstName, lastName, dateOfBirth, null, clientId, RegistrationStatus.ADDED);
        personRepository.save(child);
        assignRoleAndPermissionToPerson(clientId, child.getId(), "Jungbenutzer");
        return child;
    }

    /**
     * add new parent to child
     */
    public void addGuardian(Long childId, Person parent) {

         personRepository.findById(childId)
            .ifPresent(child -> {
                child.getLegalGuardians().add(parent);
                personRepository.save(child);
            } );

    }


    /**
     * Save Person
     */
    public Person save(Person person)
    {
        if (person == null) {
            return null;
        }
        return personRepository.save(person);
    }

    /**
     * Find person by id
     */
    public Person findById(long id)
    {
        return personRepository.findById(id).orElse(null);
    }


    /**
     * get person_id of currently logged in user
     */
     public Long getPersonId()
    {
        User user = securityService.findLoggedInUser();
        return user.getPerson().getId();
    }
    /**
     * Löscht eine Person und alle ihre Assoziationen.
     * 
     * Das ist die Person und der User, falls vorhanden. Weiterhin sind es sind die clientspezifischen
     * Rollen und die Clientzuordnungen einer Person.
     * 
     * @param person_id Die Id der Person
     * @return Die Anzahl der gelöschten Datensätze insgesamt
     */
    public long deleteById(long person_id)
		{
            List<Long> associatedPersonList = userRepository.findLegalGuardiansId(person_id);
            
            if(!associatedPersonList.isEmpty()){
                userRepository.deleteByLegalGuardiansId(person_id);
            }          
        
        long count1 = 0;

        log.debug("Deleting IdPicture of Person.id = " + person_id);
        idPictureRepository.deleteByPersonId(person_id);

        // Löschen der Clients
        List<Client> clientList = clientRepository.findByPersonId(person_id);
        if (!clientList.isEmpty()){
            for (Client client : clientList) {
                permissionService.deleteClientPerson(client.getId(), person_id);
                if (count1 == -1){
                    log.info("Es wurden keine Clients gelöscht, denen die Person mit der person_id " + person_id + " zugeordnet ist.");
                }
            }
        }
        else
        {
            log.info("Es wurden keine Clients gefunden, denen die Person mit der person_id " + person_id + " zugeordnet ist.");
        }
        
        // Löschen der Person und des Users:
        User user = userRepository.findByPersonId(person_id).orElse(null);
        if (user != null){
            userRepository.deleteById(user.getId());
        }

        personRepository.deleteById(person_id);
        return 1;
    }
    /**
     * Deletes person_associated person by id
     * @param person_id: ID of the person
     */
    public void deletePersonLegalGuardianById(long person_id)
    {
        /*
        I did not find a way to use @OneToMany(orphanRemoval=true) at the person model
        to delete the assocations automatically when the person is deleted.
        */
        List<PersonPermission> permissions = permissionRepository.findPermissionByPersonId(person_id);

        //delete role and client assocation
        for (PersonPermission permission : permissions) {
            permissionService.deleteClientPersonRole(permission);
        }
        //delete association with person
        userRepository.deleteByLegalGuardiansId(person_id);

        //finally: delete the person
        deleteById(person_id);
    }

    
    /**
     * 
     */
    public Person edit(Person person)
    {
        Person existing = personRepository.findById( person.getId() ).orElse(null);

        // not yet saved, so just save the new one
        if (existing == null) return this.save(new Person());

        existing.setFirstName( person.getFirstName() );
        existing.setLastName( person.getLastName() );

        return this.save(existing);
    }

    
    /**
     * 
     */
    public Person edit(long id, String firstName, String lastName, LocalDate birthdate, String email)
    {
        Person existing = personRepository.findById(id).orElse(null);

        // not yet saved, so just save the new one
        if (existing == null) return this.save(new Person());

        existing.setFirstName(firstName);
        existing.setLastName(lastName);
        existing.setBirthdate(birthdate);
        existing.setEmail(email);

        return this.save(existing);
    }

    /**
     * save a person registration status and update in the database
     */
    public Person edit(long id, RegistrationStatus status)
    {
        Person existing = personRepository.findById(id).orElse(null);
        // not yet saved, so just save the new one
        if (existing == null) return this.save(new Person());

        existing.setStatus(status);
       

        return this.save(existing);
    }

    public Person editPin(long id, int pin)
    {
        Person existing = personRepository.findById(id).orElse(null);

        // not yet saved, so just save the new one
        if (existing == null) return this.save(new Person());

        existing.setPin(pin);
       

        return this.save(existing);
    }


    /**
     * save edited group supervisor
     */
    public Person supervisor_person_edit(long id, String firstName, String lastName, String email)
    {
        Person existing = personRepository.findById(id).orElse(null);

        // not yet saved, so just save the new one
        if (existing == null) return this.save(new Person());

        existing.setFirstName(firstName);
        existing.setLastName(lastName);
        existing.setEmail(email);

        return this.save(existing);
    }

    
    /**
     * 
     */
    public List<Person> findAll()
    {
        return personRepository.findAll();
    }

     /**
     * Find all children by Client ID
     */
    public Set<Person> findChildrenAtClient(long client_id)
    {
        return personRepository.findChildrenByClientId(client_id);
    }
    
    /**
     * 
     */
    public Person getOne(long id) throws EntityNotFoundException
    {
        return personRepository.getOne(id);
    }

    
    /**
     * 
     */
    public void deleteInBatch(Iterable<Person> instances)
    {
        personRepository.deleteInBatch(instances);
    }


    /**
     * 
     */
    public PagingResponse<Person> getPage(PagingRequest request)
    {
        Pageable pageable = PageRequest.of(request.getPageIndex(), request.getLength(), request.getSort());
        Page<Person> page = personRepository.findAll(pageable);

        // TODO: update second page.getTotalElements() to something for Sort
        return new PagingResponse<>(request.getDraw(), page.getTotalElements(), page.getTotalElements(), page.getContent());
    }

    public Person findByEmail(String email)
    {
        return personRepository.findByEmail(email).orElse(null);
    }

    public Person findByEmailAndClientId(String email, Long client_id) {
        return personRepository.findByEmailAndClientId(email, client_id).orElse(null);
    }

}
