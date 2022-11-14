package com.klix.backend.repository;

import java.util.List;
import java.util.Optional;

import com.klix.backend.model.Client;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


/**
 * 
 */
public interface ClientRepository extends JpaRepository<Client, Long>
{
    Optional<Client> findByName(String name);

    @Query(value = "SELECT id, name FROM client", nativeQuery = true)
    List<Client> getAllNames();

    /**
     * Client einer Person
     */
    static final String SELECT_CUSTOMER_QUERY = " SELECT *"
                                               +"       FROM client c"
                                               +" INNER JOIN client_person cp"
                                               +"         ON cp.client_id = c.id";

    @Query(value = SELECT_CUSTOMER_QUERY + " WHERE cp.person_id = :person_id", nativeQuery = true)
    public List<Client> findByPersonId(@Param("person_id") Long personId);
}