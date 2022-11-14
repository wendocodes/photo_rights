package com.klix.backend.repository;

import java.util.List;
import java.util.Optional;

import com.klix.backend.model.User;
import com.klix.backend.repository.projections.IdUsernameAndEmail;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;


/**
 * Repository für User-Models
 */
public interface UserRepository extends JpaRepository<User, Long>
{
    Optional<User> findByUsername(String username);

    Optional<User> findByPersonId(Long personId);

    @Query(value = "SELECT roles_id FROM user_roles WHERE user_id = :user_id", nativeQuery = true)
    List<Long> findRoleIdsByUserId(@Param("user_id") Long userId);

    @Query(value = "SELECT id, username, email FROM user", nativeQuery = true)
    Page<IdUsernameAndEmail> findPage(Pageable pageable);

    @Query(value = "SELECT * FROM user", nativeQuery = true)
    List<IdUsernameAndEmail> getAllIdUsernameAndEmail();
 
    @Query(value = "SELECT legal_guardians_id FROM person_legal_guardians", nativeQuery = true)
    List<Long> findLegalGuardiansId(@Param("legal_guardians_id") Long LegalGuardiansId);

    @Modifying
    @Transactional 
    @Query(value = "DELETE FROM person_legal_guardians WHERE legal_guardians_id = :legal_guardians_id", nativeQuery = true)
    void deleteByLegalGuardiansId(@Param("legal_guardians_id") Long LegalGuardiansId);

    @Query("SELECT u FROM User u WHERE u.email = ?1")
    Optional<User> findByEmail(String email);
   
    Optional<User> findByResetPasswordToken(String token);
}