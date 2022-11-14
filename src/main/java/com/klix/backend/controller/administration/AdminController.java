package com.klix.backend.controller.administration;

import com.klix.backend.model.Client;
import com.klix.backend.model.Person;
import com.klix.backend.model.User;
import com.klix.backend.repository.projections.PersonPermission;
import com.klix.backend.validators.groups.ClearPassword;
import com.klix.backend.constants.RoleConstants;
import com.klix.backend.controller.BaseController;
import com.klix.backend.model.ClientRole;

import java.util.List;
import java.util.Locale;

import java.lang.IllegalArgumentException;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@Controller
@RequestMapping("/administration/*")
public class AdminController extends BaseController
{
    /**
     * Die Funktion liefert den Treeview mit den Personen-Client-Rollen
     * Assoziationen.
     * 
     * @param Das Model für den View
     * @return Der Thymeleaf-View
     */
    @GetMapping("/permission_table")
    public String administration_permission_table(Model model) {

        //add from adress to View to redirect back
        model.addAttribute("from", "administration/permission_table");

        return "administration/permission_table";
    }
    

    /**
     * Die Funktion liefert die Tabelle mit den Benutzern.per
     * 
     * @param Das Model für den View
     * @return Der Thymeleaf-View
     */
    @GetMapping("/user_table")
    public String administration_user_table(Model model, Locale locale)
    {
        model.addAttribute("tableTitle", messageSource.getMessage("root.administration.usertable.title", null, locale));
        model.addAttribute("columns", new String[][]{
            new String[]{"Id", "id"},
            new String[]{"Username", "username"},
            new String[]{"Email", "email"},
        });
        model.addAttribute("model", "user");
        model.addAttribute("modelPlural", "users");

        model.addAttribute("title", messageSource.getMessage("root.administration.usertable.title", null, locale));

        //add from adress to View to redirect back
        model.addAttribute("from", "administration/user_table");

        return "administration/model_table";
    }


    /**
     * Table views (Users & CLients)
     */
    @GetMapping("/client_table")
    public String administration_client_table(Model model, Locale locale)
    {
        model.addAttribute("tableTitle", messageSource.getMessage("root.administration.clienttable.title", null, locale));
        model.addAttribute("columns", new String[][]{
            new String[]{"Id", "id"},
            new String[]{messageSource.getMessage("root.add_client.name", null, locale), "name"},
        });
        model.addAttribute("model", "client");
        model.addAttribute("modelPlural", "clients");

        model.addAttribute("title", messageSource.getMessage("root.administration.clienttable.title", null, locale));

        //add from adress to View to redirect back
        model.addAttribute("from", "administration/client_table");

        return "administration/model_table";
    }


    /**
     * Table views (Users & CLients)
     */
    @GetMapping("/person_table")
    public String administration_person_table(Model model, Locale locale)
    {
        model.addAttribute("tableTitle", messageSource.getMessage("root.administration.persontable.title", null, locale));
        model.addAttribute("columns", new String[][]{
            new String[]{"Id", "id"},
            new String[]{messageSource.getMessage("root.add_person.firstNameLabel", null, locale), "firstName"},
            new String[]{messageSource.getMessage("root.add_person.lastNameLabel", null, locale), "lastName"}
        });

        model.addAttribute("from", "administration/person_table");

        model.addAttribute("model", "person");
        model.addAttribute("modelPlural", "persons");

        model.addAttribute("title", messageSource.getMessage("root.administration.persontable.title", null, locale));

        return "administration/model_table";
    }


    /**
     *  Hinzufügen eines Users
     */
    @GetMapping("/user_add")
    public String add_user(Model model)
    {
        // render form view with empty fields
        model.addAttribute("user", new User());
        return "administration/user_add";
    }


    /**
     * Die Methode fügt einen User anhand der submitteten Form an und gibt den "user-table" View zurück.
     * 
     * @param Das Model für den View
     * @return Der Thymeleaf-View
     */
    @PostMapping("/user_add")
    public String add_user(@Validated(ClearPassword.class) @ModelAttribute("user") User user, Errors errors,  @ModelAttribute("from") String from)
    {
        // on error: stay on page and display errors for a good user experience
        if (errors.hasErrors()) return "administration/user_add";
        
        // create new instance and set its role to a generic user
        userService.addRole(user, RoleConstants.user);
        userService.create(user);

        // redirect to table view of users
        return "redirect:/"+from;
    }


    /**
     * Die Methode liefert den View für das Anlegen eines Benutzers für eine vorhandene Person.
     * 
     * @param Das Model für den View
     * @return Der Thymeleaf-View
     */
    @GetMapping("/user_add/{personId}")
    public String user_add_for_person(Model model, @PathVariable("personId") Long personId, @ModelAttribute("from") String from)
    {
        // Basic Guards
        if(!checkPersonId(personId)) throw new IllegalArgumentException();

        // add user data to model
        User user = new User();

        Person person = personService.findById(personId);

        user.setPerson(person);
        model.addAttribute("user", user);

        model.addAttribute("from", from);
        return "administration/user_add";
    }


    /**
     * View for adding a client for an already existing person
     * add (Create)
     */
    @GetMapping("/person_add_client/{personId}")
    public String add_client_person(Model model, @PathVariable("personId") Long personId, Locale locale) 
    {
        // Basic Guards
        if(!checkPersonId(personId)) throw new IllegalArgumentException();
        
        // get all information to decide which client can still be associated with this person
        List<Client> clients = clientService.findAll();
        List<PersonPermission> permissions = permissionRepository.findPermissionByPersonId(personId);
        boolean availableClient = false;

        // remove those clients from clients list if they are already associated with this person
        if(permissions != null) 
        {
            for (PersonPermission personPermission : permissions) 
            {
                if( personPermission.getClient_id() != null && 
                    clientService.findById(personPermission.getClient_id()) != null) 
                {
                        Client client = clientService.findById(personPermission.getClient_id());
                        if(clients.contains(client)) clients.remove(client);
                }     
            }
        }

        //if all clients are already associated, then don't show multiple select
        if(!clients.isEmpty()){
            availableClient = true;
            model.addAttribute("availableClient", availableClient);
        } 
        
        //render view
        model.addAttribute("clients", clients);
        model.addAttribute("personId", personId);

        return "administration/person_add_client";
    }


    /**
     * 
     */
    @PostMapping("/person_add_client/{personId}")
    public String add_client_person(@ModelAttribute("clients") List<String> clientsId, @PathVariable("personId") Long personId) 
    {
        // Basic Guards
        if(!checkPersonId(personId)) throw new IllegalArgumentException();

        for (String clientId : clientsId) {
            permissionRepository.insertClientPerson(Long.parseLong(clientId), personId);
        }
        return "administration/permission_table";
    }

 
    /**
     *  add (Create)
     */
    @GetMapping("/client_add")
    public String add_client(Model model) 
    {
        // render form view with empty fields
        model.addAttribute("client", new Client());
        return "administration/client_add";
    }


    /**
     *  add (Create)
     */
    @PostMapping("/client_add")
    public String add_client(@Validated @ModelAttribute("client") Client client, Errors errors)
    {
        // on error: stay on page and display errors for a good user experience
        if (errors.hasErrors()) return "administration/client_add";

        // create new instance and redirect to table view of clients
        clientService.save(client);
        return "redirect:/administration/client_table";
    }


    /**
     *  add (Create)
     */
    @GetMapping("/person_add")
    public String add_person(Model model) 
    {
        model.addAttribute("person", new Person());
        return "administration/person_add";
    }


    /**
     *  add (Create)
     */
    @PostMapping("/person_add")
    public String add_person(@Validated @ModelAttribute("person") Person person, Errors errors)
    {
        // on error: stay on page and display errors for a good user experience
        if (errors.hasErrors()) return "administration/person_add";

        // create new instance and redirect to table view of clients
        personService.save(person);
        return "redirect:/administration/person_table";
    }


    /**
     *  add (Create)
     */
    @GetMapping("/permission_add/{personId}/{clientId}")
    public String add_permission(Model model,
                                @PathVariable("personId") Long personId, 
                                @PathVariable("clientId") Long clientId)
    {
        // Basic Guards
        if(!checkPersonId(personId)) throw new IllegalArgumentException();
        if(!checkClientId(clientId)) throw new IllegalArgumentException();

        //get all information to present available roles for this person
        Long client_person_id = permissionRepository.findByClientPersonId(clientId, personId);                         
        List<Long> rolesId = permissionRepository.findClientPersonRoleById(client_person_id);
        List<ClientRole> clientRoles = clientRoleRepository.findAll();
        boolean availableRole = true;
        
        //go through all roles and remove the roles which this person already has
        for (Long roleId : rolesId) {
            ClientRole role = clientRoleRepository.findById(roleId).orElse(null);
            if(clientRoles.contains(role)) clientRoles.remove(role);
        }

        //if no roles are available, don't show multiple select in view
        if(clientRoles.isEmpty()) availableRole = false;
                                 

        // render form view with empty fields
        model.addAttribute("clientRoles", clientRoles);
        model.addAttribute("availableRole", availableRole);

        return "administration/person_add_clientRole";
    }


    /**
     *  add (Create)
     */
    @PostMapping("/permission_add/{personId}/{clientId}")
    public String add_permission(   @ModelAttribute("clientRoleId") Long clientRoleId,
                                    @PathVariable("personId") Long personId,
                                    @PathVariable("clientId") Long clientId)
    {  
        // Basic Guards
        if(!checkPersonId(personId)) throw new IllegalArgumentException();
        if(!checkClientId(clientId)) throw new IllegalArgumentException();

        //insert new role to client_person_role table
        permissionRepository.insertClientPersonRole(
                        permissionRepository.findByClientPersonId(clientId, personId), clientRoleId);

        // redirect to table view of users
        return "administration/permission_table";
    }


    /**
     * Edit (Update)
     */
    @PostMapping("/user_edit")
    //public String save_editedUser(@ModelAttribute("userForm") User userForm, BindingResult bindingResult) 
    public String save_editedUser(@Validated(ClearPassword.class) @ModelAttribute("user") User user, Errors errors , @ModelAttribute("from") String from) 
    {
        // On error: stay on page and display errors for a good user experience
        if (errors.hasErrors())  return "administration/user_edit";

        // Save changes in db and redirect to table view
        userService.edit(user);
        
        //use from parameter from url to redirect back
        return "redirect:/"+from;
    }


    /**
     * Editieren eines Users mit der gegebenen Id 
     */
    @GetMapping("/user_edit/{id}")
    public String showForm_editUser(Model model, @PathVariable("id") Long userId, @ModelAttribute("from") String from)
    {
        if(!checkUserId(userId)) throw new IllegalArgumentException();

        // Add the user data to the model:
        User user = userService.findById(userId);

        model.addAttribute("user", user);
        return "administration/user_edit";
    }


    /**
     * Edit (Update)
     */
    @PostMapping("/client_edit")
    //public String save_editedClient(@ModelAttribute("clientForm") Client clientForm, BindingResult bindingResult) 
    public String save_editedClient(@Validated @ModelAttribute("client") Client client, Errors errors, @ModelAttribute("from") String from) 
    {
        // on error: stay on page and display errors for a good user experience
        if (errors.hasErrors())  return "administration/client_edit";

        // save changes in db and redirect to table view
        clientService.edit(client.getId(), client.getName());       
        
        //use from parameter from url to redirect back
        return "redirect:/"+from;
    }


    /**
     * Request to edit client
     * 
     * @param Das Model für den View
     * @param Die Id des zu editierenden Client 
     * @return Der Thymeleaf-View
     */
    @GetMapping("/client_edit/{id}")
    public String showForm_editClient(Model model, @PathVariable("id") long clientId, @ModelAttribute("from") String from)
    {
        if(!checkClientId(clientId)) throw new IllegalArgumentException();

        // add client data to model
        Client client = clientService.findById(clientId);

        model.addAttribute("client", client);

        // render form
        return "administration/client_edit";
    }


    /**
     * Edit (Update)
     */
    @PostMapping("/person_edit")
    public String save_editedPerson(@ModelAttribute("client") Person person, BindingResult bindingResult, @ModelAttribute("from") String from) 
    {
        personService.edit(person.getId(), person.getFirstName(), person.getLastName(), person.getBirthdate(), person.getEmail());
        //use from parameter from url to redirect back
        return "redirect:/"+from;
    }


    /**
     * Edit (Update)
     */
    @GetMapping("/person_edit/{id}")
    public String showForm_editPerson(Model model, @PathVariable("id") long personId, @ModelAttribute("from") String from)
    {
        if(!checkPersonId(personId)) throw new IllegalArgumentException();
        
        // add client data to model
        Person person = personService.findById(personId);
        
        model.addAttribute("person", person);

        // render form
        return "administration/person_edit";
    }

    /**************************** Delete ******************************************/
    // see DataTableController (is a RestController, therefore its functions don't return rendered templates. Delete has no dedicated view)
}