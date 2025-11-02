package com.example.authorization.repository;

import com.example.authorization.model.Credential;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
public class CredentialRepository {
    private static final Logger logger = LoggerFactory.getLogger(CredentialRepository.class);

    private final ObjectMapper objectMapper;
    private final Resource dataResource;
    private List<Credential> credentials = Collections.emptyList();

    public CredentialRepository(ObjectMapper objectMapper,
                                @Value("classpath:data/credentials.json") Resource dataResource) {
        this.objectMapper = objectMapper;
        this.dataResource = dataResource;
    }

    @PostConstruct
    void load() {
        try (InputStream inputStream = dataResource.getInputStream()) {
            credentials = objectMapper.readValue(inputStream, new TypeReference<>() {});
            logger.info("Loaded {} credential entries", credentials.size());
        } catch (IOException e) {
            logger.error("Failed to load credentials", e);
            credentials = Collections.emptyList();
        }
    }

    public Optional<Credential> findByUsername(String username) {
        return credentials.stream()
                .filter(credential -> credential.getUsername().equalsIgnoreCase(username))
                .findFirst();
    }
}
