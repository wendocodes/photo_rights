package com.klix.backend.repository;

import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;

import com.klix.backend.model.IdPicture;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


/**
 * Persistenz der Referenzbilder
 */
public interface IdPictureRepository extends JpaRepository<IdPicture, Long>
{
    /*      QUERIES     */
    static final String SELECT_IDPICTURE_QUERY =  "SELECT * FROM id_picture LEFT JOIN client_person ON id_picture.person_id = client_person.person_id ";

    Optional<IdPicture> findByPersonId(long personId);

    @Transactional
    long deleteByPersonId(long personId);

    @Query(value = SELECT_IDPICTURE_QUERY+"WHERE client_person.client_id=:client_id" , nativeQuery = true)
    public List<IdPicture> findByClientId(@Param("client_id") Long clientId);
    
    @Query(value = "SELECT * FROM id_picture WHERE picture_id = : picture_id" , nativeQuery = true)
    public List<IdPicture> findPersonIdsByPictureId(@Param("client_id") Long clientId);

}

