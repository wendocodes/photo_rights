package com.klix.backend.repository;

import java.util.List;
import java.util.Optional;

import com.klix.backend.model.ClientRole;
import com.klix.backend.model.Person;
import com.klix.backend.repository.projections.PersonPermission;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;


@Transactional
public interface PermissionRepository extends JpaRepository<Person, Long>
{
    /*      QUERIES     */

    // basic select-query for projection PersonPermission
    static final String SELECT_ROLES_QUERY =
         " SELECT p.id as person_id, p.last_name, p.first_name,"
        +"            c.name as client_name, cr.name as client_role_name,"
        +"            cp.client_id, cpr.client_role_id, cp.id as client_person_id, cpr.id as id"
        +"       FROM person p"
        +"  LEFT JOIN client_person cp"
        +"         ON p.id = cp.person_id"
        +"  LEFT JOIN client c"
        +"         ON cp.client_id = c.id"
        +"  LEFT JOIN client_person_role cpr"
        +"         ON cp.id = cpr.client_person_id"
        +"  LEFT JOIN client_role cr"
        +"         ON cpr.client_role_id = cr.id";
    
    static final String INSERT_CLIENT_PERSON_QUERY =
        "INSERT INTO client_person (client_id, person_id) VALUES";

    static final String INSERT_CLIENT_PERSON_ROLE_QUERY =
        "INSERT INTO client_person_role (client_person_id, client_role_id) VALUES";

    static final String UPDATE_CLIENT_PERSON_QUERY =
        "UPDATE client_person SET client_id = :client_id, person_id = :person_id";

    static final String UPDATE_CLIENT_PERSON_ROLE_QUERY =
        "UPDATE client_person SET client_person_id = :client_person_id, client_role_id = :client_role_id";


    /*      SELECT FUNCTIONS       */

    /*      find all    */

    @Query(value = SELECT_ROLES_QUERY, nativeQuery = true)
    public List<PersonPermission> findAllPermissions();

    // wird im permission service benutzt! ( pageable klappt also mit pojos ! )
    @Query(value = SELECT_ROLES_QUERY, nativeQuery = true)
    public Page<PersonPermission> findAllPermissions(Pageable pageable);

    @Query(value = "SELECT id, name FROM ClientRole", nativeQuery = false)
    public List<ClientRole> findAllRoles();



    /*      find one by id      */

    @Query(value = SELECT_ROLES_QUERY + " WHERE cpr.id = :id" , nativeQuery = true)
    public Optional<PersonPermission> findPermissionById(@Param("id") Long id);

    @Query(value = "SELECT id FROM client_person WHERE client_id = :client_id AND person_id = :person_id", nativeQuery = true)
    public Long findByClientPersonId(@Param("client_id") Long clientId, @Param("person_id") Long personId);


    /*      find by multiple member-ids      */

    @Query(value =  SELECT_ROLES_QUERY + " WHERE person_id = :person_id AND client_id = :client_id AND client_role_id = :client_role_id", nativeQuery = true)
    public Optional<PersonPermission> findPermissionByPersonAndClientAndRole(@Param("person_id") Long personId, @Param("client_id") Long clientId, @Param("client_role_id") Long clientRoleId);

    @Query(value = SELECT_ROLES_QUERY + " WHERE person_id = :person_id AND client_id = :client_id", nativeQuery = true)
    public List<PersonPermission> findPermissionsByPersonAndClient(@Param("person_id") Long personId, @Param("client_id") Long clientId);

    @Query(value = SELECT_ROLES_QUERY + " WHERE client_role_id IN :client_role_ids AND client_id = :client_id", nativeQuery = true)
    public List<PersonPermission> findPermissionsByRolesAndByClient(@Param("client_role_ids") Iterable<Long> client_role_ids, @Param("client_id") Long client_id);

    @Query(value = SELECT_ROLES_QUERY + " WHERE client_role_id IN :client_role_ids AND client_id = :client_id ORDER BY person_id", nativeQuery = true)
    public List<PersonPermission> findPermissionsByRolesAndByClientOrderByPersonId(@Param("client_role_ids") Iterable<Long> client_role_ids, @Param("client_id") Long client_id);

    @Query(value = "SELECT client_role_id FROM client_person_role WHERE client_person_id = :client_person_id", nativeQuery = true)
    public List<Long> findClientPersonRoleById(@Param("client_person_id") Long clientPersonId);
    
    /*      find list by one member-id      */

    // by role
    @Query(value = SELECT_ROLES_QUERY + " WHERE cpr.client_role_id = :role_id" , nativeQuery = true)
    public List<PersonPermission> findPermissionByRoleId(@Param("role_id") Long roleId);

    @Query(value = SELECT_ROLES_QUERY + " WHERE cpr.client_role_id = :role_id" , nativeQuery = true)
    public List<PersonPermission> findPermissionByRoleId(@Param("role_id") Long roleId, Pageable pageable);

    // by client
    @Query(value = SELECT_ROLES_QUERY + " WHERE cp.client_id = :client_id" , nativeQuery = true)
    public List<PersonPermission> findPermissionByClientId(@Param("client_id") Long clientId);

    @Query(value = SELECT_ROLES_QUERY + " WHERE cp.client_id = :client_id" , nativeQuery = true)
    public List<PersonPermission> findPermissionByClientId(@Param("client_id") Long clientId, Pageable pageable);

    // by person
    @Query(value = SELECT_ROLES_QUERY + " WHERE p.id = :person_id" , nativeQuery = true)
    public List<PersonPermission> findPermissionByPersonId(@Param("person_id") Long personId);

    @Query(value = SELECT_ROLES_QUERY + " WHERE p.id = :person_id" , nativeQuery = true)
    public List<PersonPermission> findPermissionByPersonId(@Param("person_id") Long personId, Pageable pageable);


    /*      INSERTING FUNCTIONS      */

    @Modifying
    @Query(value = INSERT_CLIENT_PERSON_QUERY + " (:client_id, :person_id)" , nativeQuery = true)
    public int insertClientPerson(@Param("client_id") Long clientId, @Param("person_id") Long personId);

    @Modifying
    @Query(value = INSERT_CLIENT_PERSON_ROLE_QUERY + " (:client_person_id, :client_role_id)" , nativeQuery = true)
    public int insertClientPersonRole(@Param("client_person_id") Long clientPersonId, @Param("client_role_id") Long clientRoleId);


    /*      UPDATING FUNCTIONS      */

    @Modifying
    @Query(value = UPDATE_CLIENT_PERSON_QUERY + " WHERE id = :id", nativeQuery = true)
    public boolean updateClientPerson(@Param("id") Long id, @Param("client_id") Long clientId, @Param("person_id") Long personId);

    @Modifying
    @Query(value = UPDATE_CLIENT_PERSON_ROLE_QUERY + " WHERE id = :id" , nativeQuery = true)
    public boolean updateClientPersonRole(@Param("id") Long id, @Param("client_person_id") Long clientPersonId, @Param("client_role_id") Long clientRoleId);


    /*      DELETING FUNCTIONS        */

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM client_person WHERE client_id = :client_id AND person_id = :person_id", nativeQuery = true)
    public int deleteClientPersonByIds(@Param("client_id") Long clientId, @Param("person_id") Long personId);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM client_person_role WHERE id = :client_person_role_id", nativeQuery = true)
    public int deleteClientPersonRoleById(@Param("client_person_role_id") Long clientPersonRoleId);
}
