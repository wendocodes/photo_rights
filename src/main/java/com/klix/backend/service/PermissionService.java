package com.klix.backend.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.klix.backend.model.paging.PersonPermissionPage;
import com.klix.backend.repository.ClientRepository;
import com.klix.backend.repository.PermissionRepository;
import com.klix.backend.repository.PersonRepository;
import com.klix.backend.repository.projections.PersonPermission;
import com.klix.backend.repository.projections.PersonPermissionImp;
import com.klix.backend.service.user.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

import com.klix.backend.model.Client;
import com.klix.backend.model.Person;
import com.klix.backend.model.User;
import com.klix.backend.model.treeview.Node;


/**
 * Personen-Client-Rollen (Permission) spezifische Rollen
 */
@Service
@Slf4j
public class PermissionService
{
    @Autowired private UserService userService;

    @Autowired private PermissionRepository permissionRepository;

    @Autowired private PersonRepository personRepository;

    @Autowired ClientRepository clientRepository;


    /**
     *
     */
    public PersonPermission edit(PersonPermission permission)
    {
        if (permission == null)
        {
            log.warn("Permission not found on save. Exit.");
            return null;
        }

        if (permission.getClient_id() == null || permission.getPerson_id() == null || permission.getClient_role_id() == null)
        {
            //throw new Exception("Parameter in PermissionServiceImp edit sind nicht gegeben.");
            log.warn("Permission Foreign Keys in PermissionService edit sind nicht gegeben.");

            // !TODO better communication to caller that saving didnt work? No one will remember to check if id == null or something afterwards
            return permission;
        }
        else
        {
            PersonPermission existing = permissionRepository.findPermissionById(permission.getId()).orElse(null);

            // create new
            if (existing == null){
                PersonPermission newRole = new PersonPermissionImp(permission);
                return this.save(newRole);
            }

            // update existing
            return this.save(existing);
        }       
    }

    
    /**
     *
     */
    public PersonPermission save(PersonPermission permission)
    {
        if (permission == null)
        {
            log.info("Permission not found on save. Exit.");
            return null;
        }

        // insert or update ClientPerson
        if (permission.getClient_person_id() != null){
            permissionRepository.updateClientPerson(permission.getClient_person_id(),
                                                    permission.getClient_id(),
                                                    permission.getPerson_id());
        }
        else{
            permissionRepository.insertClientPerson(permission.getClient_id(),
                                                    permission.getPerson_id());
        }

        // insert or update ClientPersonRole
        Long permissionId = null;
        if (permission.getId() != null){
            permissionId = permission.getId();
            permissionRepository.updateClientPersonRole(permission.getId(),
                                                        permission.getClient_person_id(),
                                                        permission.getClient_role_id());
        }
        else{
            permissionRepository.insertClientPersonRole(permission.getClient_person_id(), permission.getClient_role_id());

            return permissionRepository.findPermissionByPersonAndClientAndRole(permission.getPerson_id(),
                                                                                permission.getClient_id(),
                                                                                permission.getClient_role_id()).orElse(null);
        }

        // return updated, persisted version
        return permissionRepository.findPermissionById(permissionId).orElse(null);
    }

    
    /**
     *
     */
    public PersonPermission findById(long id) {
        return permissionRepository.findPermissionById(id).orElse(null);
    }

    
    /**
     *
     */
    public List<PersonPermission> findAll() {
        return permissionRepository.findAllPermissions();
    }

    
    /**
     *
     */
    public List<Long> findPermissionIdsByMemberIds(Long role_id, Long client_id, Long person_id)
    {
        Map<Long, Integer> ids = new HashMap<>();
        int filterNum = 0;

        if (role_id != null){
            filterNum++;

            for (PersonPermission p : permissionRepository.findPermissionByRoleId(role_id))
                ids.put( p.getId(), ids.getOrDefault(p.getId(), 0) + 1);

        }
        if (client_id != null){
            filterNum++;

            for (PersonPermission p : permissionRepository.findPermissionByClientId(client_id))
                ids.put( p.getId(), ids.getOrDefault(p.getId(), 0) + 1);
        }
        if (person_id != null){
            filterNum++;

            for (PersonPermission p : permissionRepository.findPermissionByPersonId(person_id))
                ids.put( p.getId(), ids.getOrDefault(p.getId(), 0) + 1);
        }
        List<Long> permissionIds = new ArrayList<>();
        for (Map.Entry<Long, Integer> e : ids.entrySet()) if (e.getValue() == filterNum) permissionIds.add( e.getKey() );

        return permissionIds;
    }


    /**
     * Löschen der Person, der clientspezifischen Rollen und der Clientzuordnung
     * einer Person anhand der Personen-Id inklusive des Users.
     * 
     * @return Die Anzahl der gelöschten Datensätze
     */
    
    public long deletePerson(Long person_id)
    {
        long count1 = 0;

        // Löschen der Clients
        List<Client> clientList = clientRepository.findByPersonId(person_id);
        if (!clientList.isEmpty())
        {
            for (Client client : clientList) {
                deleteClientPerson(client.getId(), person_id);
                if (count1 == -1){
                    log.warn("Es wurden keine Client gelöscht, denen die Person mit der person_id " + person_id + " zugeordnet ist.");
                }
            }
        }
        else
        {
            log.warn("Es wurden keine Client gefunden, denen die Person mit der person_id " + person_id + " zugeordnet ist.");
        }

        //Löschen der Person und des Users
        User user = userService.findByPersonId(person_id);
        if (user != null)
            userService.deleteById(user.getId());

        personRepository.deleteById(person_id);
        return count1 + 1;
    }

    
    
    /**
     *  Löschen der clientspezifischen Rollen und der Clientzuordnung einer Person anhand der Key Id's
     * 
     * @return Die Anzahl der gelöschten Datensätze
     * */
     
    public long deleteClientPerson(Long client_id, Long person_id)
    {

        long count = 0;
        
        log.debug("Loeschen der Client-Person Assoziation in deleteClientPerson: client_id " + client_id + " person_id " + person_id);
        if (client_id == null || person_id == null)
        {
            log.warn("At least one Param is null in deleteClientPerson(): client_id: " + client_id + ", person_id: " + person_id);
            return -1;
        }

        // Löschen der Rollen beim Client
        List<PersonPermission> permissionList = permissionRepository.findPermissionsByPersonAndClient(person_id, client_id);
        
        // info if none found
        if (permissionList.isEmpty())
        {
            // Logging wenn keine Permissions/Rollen gefunden wurden
            log.info("Es wurden keine Rollen für die Person mit der person_id " +
                      person_id + " beim Client client_id " +
                      client_id + " gefunden.");
        }


        //Löschen des Client
        count = permissionRepository.deleteClientPersonByIds(client_id, person_id);
        if (count == -1)
        {
            // Logging wenn kein zugeordneter Client gefunden wurde
            log.warn("Es wurden kein zugeordneter Client für die Person mit der person_id " +
                      person_id + " mit der client_id " +
                      client_id + " gefunden.");
            return -1;
        }

        // sum of deleted rows in ClientPerson and ClientPersonRoles
        return count;
    }

    
    
    /**
     * Löschen einer clientspezifischen Rolle einer Person anhand der Key Id's
     * 
     * @return Die Anzahl der gelöschten Datensätze
     */
    public long deleteClientPersonRole(Long client_role_id, Long client_id, Long person_id)
    {
        log.debug("Loeschen der deleteClientPersonRole assoziation: " +
                  " client_role_id = " + client_role_id +
                  " client_id " + client_id +
                  " person_id " + person_id);
                  
        if (client_role_id == null || client_id == null || person_id == null)
        {
            log.warn("At least one Param is null in deleteClientPerson(): client_role_id = " + client_role_id +
                  ", client_id " + client_id +
                  ", person_id " + person_id);
            return -1;
        }

        PersonPermission permission = permissionRepository.findPermissionByPersonAndClientAndRole(
            person_id,
            client_id,
            client_role_id).orElse(null);

        long count = deleteClientPersonRole(permission);
        log.debug("deleteClientPersonRole hat " + count + " Datensätze gelöscht");

        // Logging wenn keine Permission/Rolle gefunden wurde
        if (count == -1)
        {
            log.warn("Die Rolle mit dem Key client_role_id = " + client_role_id +
                     " client_id " + client_id +
                     " person_id " + person_id +
                     " wurde nicht gefunden.");
        }

        return count;
    }


    /**
     *  Löschen einer clientspezifischen Rolle einer Person anhand des PersonPermission Models
     * 
     * @return Die Anzahl der gelöschten Datensätze
     */
    public long deleteClientPersonRole(PersonPermission personPermission)
    {
        // Test ob eine clientspezifische Rolle für die Person vorhanden ist:
        if (personPermission != null && personPermission.getId() != null)
        {
            Long id = personPermission.getId();
            log.debug("Client_person_role Table Id: " + String.valueOf(id));

            // if null, instance hsant benn persisted jet. Pretend to delete persisted entry.
            if (id == null)
            {
                log.info("PersonPermission has an id of null in deleteClientPersonRole(). Pretend to have deleted it");
                return 1;
            }

            return permissionRepository.deleteClientPersonRoleById(id);
        }
        return -1;
    }


    /**
     *
     */
    public PersonPermission getOne(long id)
    {
        return permissionRepository.findPermissionById(id).orElse(null);
    }

      
    /**
     *
     */
    public Page<PersonPermission> getPage(PersonPermissionPage page)
    {
        Pageable pageable = PageRequest.of(page.getPageNumber(), page.getPageSize(), page.getSortDirection(), page.getSortBy());
        return permissionRepository.findAllPermissions(pageable);
    }

    
    /**
     * Diese Methode baut die Datenstruktur für den Treeview mit hilfe des Node-Objekts auf.
     * 
     * Datenstruktur:
     * 
     * Person Name
     * |-> Client
     * |  |-> Role
     * ...
     * 
     * @see Node.java
     * @see Typescript ressources/static/ts/administration/permission_table.ts
     * @see AdminRestController.java, list_permissions()
     */
    public List<Node> getNodeList(PersonPermissionPage page)
    {
        List<Node> nodes = new ArrayList<>();
        String lastPID = "";
        String lastMID = "";

        // Structure permissions in a tree, using an inherently ordered list:
        for (PersonPermission permission : getPage(page).getContent())
        {
            Long personId = permission.getPerson_id();
            String p_id = String.format("p-%d", personId);

            // if the person hasnt been before, construct a root node for it
            if (!lastPID.equals(p_id)) nodes.add( getPersonNode(permission) );

            String m_id = String.format("m-%d-%d", permission.getPerson_id(), permission.getClient_id());

            // if the client hasnt been before and exists, construct a node for it
            if (!lastMID.equals(m_id) && permission.getClient_id() != null)
            {
                    // assemble client entry
                    nodes.add(
                        new Node(m_id,
                                p_id,
                                String.format("%s", permission.getClient_name()),
                                "",
                                "/administration/client_edit/" + permission.getClient_id(),
                                "/administration/permission_add/" + permission.getPerson_id() + "/" + permission.getClient_id(),
                                true,
                                true)
                    );
            }

            // if the role exists, construct a node for it
            if (permission.getClient_role_id() != null)
            {
                // assemble role entry
                nodes.add(
                    new Node(String.format("r-%d-%d-%d", permission.getPerson_id(), permission.getClient_id(), permission.getClient_role_id()),
                            m_id,
                            String.format("%s", permission.getClient_role_name()),
                            "",
                            "",
                            "",
                            true,
                            true)
                );
            }

            // set vals of this permission for next permission
            lastPID = p_id;
            lastMID = m_id;
        }
        return nodes;
    }


    /**
     * add new person node: collect fields for person node, construct and add it
     * 
     * @param permission    the base information to construct a person row
     * @return  a person row as a Node object
     */
    private Node getPersonNode(PersonPermission permission)
    {
        Long personId = permission.getPerson_id();

        // construct full name
        String name = "-";
        
        if (permission.getLast_name().length() + permission.getFirst_name().length() > 0)
        {
            name = String.format(
                "%s, %s",
                permission.getLast_name().length() > 0 ? permission.getLast_name() : "-",
                permission.getFirst_name().length()  > 0 ? permission.getFirst_name()  : "-"
            );
        }


        // prepare to display User of person
        Person person = personRepository.findById(personId).orElse(null);

        String fancyUsername = "";
        String fancyUsernameLink = "";
        String create_user_url = "";
        boolean hasUser = false;

        // add User to row if the person has one OR button to add one
        if (person != null)
        {
            User user = userService.findByPersonId(personId);
            if (user != null)
            {
                fancyUsername = user.getUsername();
                fancyUsernameLink = "/administration/user_edit/" + user.getId();
                hasUser = true;
            }
            else
            {
                create_user_url = "/administration/user_add/" + person.getId();
            }
        }

        String add_url = "/administration/person_add_client/"+ person.getId();

        // add node
        Node newNode = new Node(String.format("p-%d", personId), "0", name, fancyUsername, fancyUsernameLink, add_url);
        newNode.setHasUser(hasUser);
        newNode.setCreate_user_url(create_user_url);
        return newNode;
    }
}
