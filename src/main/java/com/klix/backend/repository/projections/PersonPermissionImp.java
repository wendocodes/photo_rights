package  com.klix.backend.repository.projections;

import lombok.Data;

/**
 * 
 */
@Data
public class PersonPermissionImp implements PersonPermission
{
    /**
     * 
     */
    public PersonPermissionImp(PersonPermission permission)
    {
        this.id = permission.getId();       // is the client_person_role_id
        this.last_name = permission.getLast_name();
        this.first_name = permission.getFirst_name();
        this.client_name = permission.getClient_name();
        this.client_role_name = permission.getClient_role_name();
        this.client_id = permission.getClient_id();
        this.person_id = permission.getPerson_id();
        this.client_role_id = permission.getClient_role_id();
        this.client_person_id = permission.getClient_person_id();
    }

    private Long id;

    private String last_name;

    private String first_name;

    private String client_name;
    
    private String client_role_name;

    private Long client_id;

    private Long person_id;

    private Long client_role_id;

    private Long client_person_id;
}