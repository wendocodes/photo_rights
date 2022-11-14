package com.klix.backend.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.klix.backend.model.Role;


/**
 * 
 */
public interface RoleRepository extends JpaRepository<Role, Long>
{
    Optional<Role> findByName(String name);
}
