package com.example.authorization.repository;

import com.example.authorization.model.Credential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CredentialRepository extends JpaRepository<Credential, Long> {
    Optional<Credential> findByUsernameIgnoreCase(String username);
}
