package com.klix.backend.controller.administration;

import java.util.HashMap;
import java.util.Map;

import com.klix.backend.controller.BaseController;
import com.klix.backend.model.Client;

import com.klix.backend.model.Person;
import com.klix.backend.model.paging.datatable.PagingRequest;
import com.klix.backend.model.paging.datatable.PagingResponse;
import com.klix.backend.model.paging.PersonPermissionPage;

import com.klix.backend.repository.projections.IdUsernameAndEmail;
import com.klix.backend.repository.projections.PersonPermission;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import java.util.Locale;

import lombok.extern.slf4j.Slf4j;


@Slf4j
@RestController
@RequestMapping("/administration/*")
public class AdminRestController extends BaseController
{
    /**
     * Request for loading permissions of persons on clients
     */
    @GetMapping("/permissions")
    public Map<String, Object> list_permissions(PersonPermissionPage page)
    {
        Map<String, Object> returnVal = new HashMap<>();

        String editBtn = messageSource.getMessage("root.administration.treeview.editBtn", null, LocaleContextHolder.getLocale());
        String addBtn = messageSource.getMessage("root.administration.treeview.addBtn", null, LocaleContextHolder.getLocale());
        String deleteBtn = messageSource.getMessage("root.administration.treeview.deleteBtn", null, LocaleContextHolder.getLocale());
        String addUserBtn = messageSource.getMessage("root.administration.treeview.addUserBtn", null, LocaleContextHolder.getLocale());

        returnVal.put("editBtn", editBtn);
        returnVal.put("addBtn", addBtn);
        returnVal.put("deleteBtn", deleteBtn);
        returnVal.put("addUserBtn", addUserBtn);

        returnVal.put("nodes", permissionService.getNodeList(page));

        return returnVal;
    }

    
    /**
     *
     */
    @GetMapping("/clients")
    public @ResponseBody Iterable<Client> list_clients()
    {
        return clientService.findAll();
    }


    /**
     *
     */
    @GetMapping("/persons")
    public @ResponseBody Iterable<Person> list_persons()
    {
        return personService.findAll();
    }

    
    /**
     * Request for loading user datatable
     */
    @PostMapping("/users_serverside")
    public PagingResponse<IdUsernameAndEmail> list_users_serverside(@RequestBody PagingRequest request)
    {
        return userService.getPage(request);
    }


    /**
     * Request for loading client datatable
     */
    @PostMapping("/clients_serverside")
    public PagingResponse<Client> list_clients_serverside(@RequestBody PagingRequest request)
    {
        return clientService.getPage(request);
    }


    /**
     * Request for loading client datatable
     */
    @PostMapping("/persons_serverside")
    public PagingResponse<Person> list_persons_serverside(@RequestBody PagingRequest request)
    {
        return personService.getPage(request);
    }


    /**
     *
     */
    @GetMapping("/users")
    public @ResponseBody Iterable<IdUsernameAndEmail> list_users()
    {
        return userService.getAllIdUsernameAndEmail();
    }


    /**
     * 
     */
    @GetMapping("/page_users")
    public org.springframework.data.domain.Page<PersonPermission> get_permissions(@RequestParam(required = false) PersonPermissionPage page)
    {
        log.debug(page == null ? "get_permissions() page is null": "get_permissions() page is not null");

        if (page == null)
        {
            page = new PersonPermissionPage();
        }
        return permissionService.getPage(page);
    }


    /**
     * Post: Diese Methode löscht ausgewählte Benutzer
     * 
     * @param ids Die Liste mit den zum Löschen ausgewahlten Id's
     */
    @PostMapping("/users_delete")
    public @ResponseBody ResponseEntity<String> delete_users(@RequestBody long[] ids, Locale locale) 
    {
        // Guards
        if (!checkUserIds(ids)) return new ResponseEntity<>("{}", HttpStatus.BAD_REQUEST);

        for (long id: ids){
            userService.deleteById(id);
        }

        // need to return something on POST
        return new ResponseEntity<>("{}", HttpStatus.OK);
    }
    

    /**
     * Post: Diese Methode löscht ausgewählte Personen
     * 
     * @param ids Die Liste mit den zum Löschen ausgewahlten Id's
     */
    @PostMapping("/persons_delete")
    public @ResponseBody ResponseEntity<String> delete_person(@RequestBody long[] ids, Locale locale) 
    {
        // Guards
        if(!checkPersonIds(ids)) return new ResponseEntity<>("{}", HttpStatus.BAD_REQUEST);

        for (long id: ids) {
            try{
                personService.deleteById(id);
            } catch(DataIntegrityViolationException e) {
                log.warn("Could not delete person at delete_person");
                String message = messageSource.getMessage("root.administration.adminRestController.errorMessage_deletePerson", null, locale);
                return new ResponseEntity<>(message, HttpStatus.INTERNAL_SERVER_ERROR);            }
        }

        return new ResponseEntity<>("{}", HttpStatus.OK);
    }

    /**
     * Post: Diese Methode löscht ausgewählte Clients
     * 
     * @param ids Die Liste mit den zum Löschen ausgewahlten Id's
     * @return 
     */
    @PostMapping("/clients_delete")
    public @ResponseBody ResponseEntity<String> delete_client(@RequestBody long[] ids, Locale locale) 
    {
        // Guards
        if(!checkClientIds(ids)) return new ResponseEntity<>("{}", HttpStatus.BAD_REQUEST);

        for (long id: ids) {
            try {
                clientService.deleteById(id);
            } catch(DataIntegrityViolationException e) {
                log.warn("Could not delete client at delete_client.");
                String message = messageSource.getMessage("root.administration.adminRestController.errorMessage_deleteClient", null, locale);
                return new ResponseEntity<>(message, HttpStatus.INTERNAL_SERVER_ERROR);            }
        }
        return new ResponseEntity<>("{}", HttpStatus.OK);
    }


    /**
     * Post: Diese Methode löscht clientspezifische Rollen
     * 
     * @param ids Die Liste mit den zum Löschen ausgewahlten Id's
     */
    @PostMapping("/permissions_delete")
    public @ResponseBody ResponseEntity<String> delete_permissions(@RequestBody Map<String, Long> ids, Locale locale)  
    {
        // TODO: Guard für die ids

        Long client_role_id = ids.get("client_role_id");
        Long client_id = ids.get("client_id");
        Long person_id = ids.get("person_id");
        log.info("Client Id : {}, person id : {}, client role id : {}, ids : {}", client_id, person_id, client_role_id, ids);
        if (client_role_id == null && client_id == null && person_id != null){
            // Personen, Clientzuordnung und Client-Rollen löschen:
            try{
                long count = personService.deleteById(person_id);
                log.debug("Beim Löschen der Person wurden " + count + " Datensätze gelöscht.");
                } catch(DataIntegrityViolationException e) {
                    log.warn("Could not delete person at /permissions_delete.");
                    String message = messageSource.getMessage("root.administration.adminRestController.errorMessage_deletePerson", null, locale);
                    return new ResponseEntity<>(message, HttpStatus.INTERNAL_SERVER_ERROR);                }
        }
        else if (client_role_id == null && checkClientId(client_id) && checkPersonId(person_id)){
            // Clientzuordnung und Client-Rollen löschen:
            try{
                permissionService.deleteClientPerson(client_id, person_id);
            }catch(DataIntegrityViolationException e) {
                log.warn("Could not delete person at /permissions_delete.");
                String message = messageSource.getMessage("root.administration.adminRestController.errorMessage_deleteClient", null, locale);
                return new ResponseEntity<>(message, HttpStatus.INTERNAL_SERVER_ERROR);            }
            
        }
        else if (client_role_id != null && checkClientId(client_id) && checkPersonId(person_id)){
            try{
            permissionService.deleteClientPersonRole(client_role_id, client_id, person_id);
            }   catch(DataIntegrityViolationException e) {
                log.warn("Could not delete person at /permissions_delete.");
                String message = messageSource.getMessage("root.administration.adminRestController.errorMessage_deletePersonRole", null, locale);
                return new ResponseEntity<>(message, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        else{
            log.warn("Map param of delete_permissions did not contain all three keys: 'role_id', 'client_id', 'person_id'. Exit.");
            return new ResponseEntity<>("{}", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        
        // need to return something on POST
        return new ResponseEntity<>("{}", HttpStatus.OK);
    }     
}