package com.klix.backend.repository;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Propagation;

import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import com.klix.backend.model.Person;

@Transactional
public interface PersonRepository extends JpaRepository<Person, Long>
{
    List<Person> findByLastName(String lastName);

    @Transactional(propagation = Propagation.REQUIRED)
    Optional<Person> findByEmail(String email);

    static final String SELECT_ROLES_QUERY =
         " SELECT cp.client_id as client_id, cp.person_id as person_id, p.id as id, p.last_name, p.first_name," 
        +" p.email as email, p.birthdate, p.status, p.pin "
        +"       FROM client_person cp"
        +"  LEFT JOIN person p"
        +"         ON  cp.person_id=p.id";
        
    @Query(value = SELECT_ROLES_QUERY+ " WHERE p.email = :email AND client_id = :client_id", nativeQuery = true)
    Optional<Person> findByEmailAndClientId(String email, Long client_id);


      // find a specific parent by pin
      @Query(value = "SELECT p.* from person p INNER JOIN client_person cp ON cp.person_id = p.id INNER JOIN person_legal_guardians plg ON cp.person_id = plg.legal_guardians_id WHERE p.pin = ?1", nativeQuery = true)

      public Person findByPin(@Param("pin") String string);

      

       
        /**
        * @author Joyce
        * @since 09.05.2022
        */
       @Query(value = "SELECT * from person p INNER JOIN client_person cp ON cp.person_id = p.id INNER JOIN person_legal_guardians pap ON p.id = pap.person_id WHERE client_id = :client_id", nativeQuery = true)

       public Set<Person> findChildrenByClientId(@Param("client_id") Long client_id);
       
       
       /**
        * @author Andreas
        * @since 11.03.2022
        */
       @Query(value="SELECT person_id FROM klix_backend_develop.person_legal_guardians WHERE legal_guardians_id=:lgid", nativeQuery = true)
       public List<Long> findChildrensIDsOfPerson (@Param("lgid") Long legalGuardianID);

}