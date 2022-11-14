package com.klix.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.klix.backend.model.Address;

public interface AddressRepository extends JpaRepository<Address, Long> {}