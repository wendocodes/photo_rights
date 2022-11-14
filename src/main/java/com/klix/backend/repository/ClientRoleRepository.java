package com.klix.backend.repository;

import java.util.Optional;

import com.klix.backend.model.ClientRole;

import org.springframework.data.jpa.repository.JpaRepository;


/**
 * 
 */
public interface ClientRoleRepository extends JpaRepository<ClientRole, Long> {

    public Optional<ClientRole> findByName(String name);
}