package com.example.authorization.controller;

import com.example.authorization.model.Credential;
import com.example.authorization.model.LoginRequest;
import com.example.authorization.model.TokenResponse;
import com.example.authorization.repository.CredentialRepository;
import com.example.authorization.service.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final CredentialRepository credentialRepository;
    private final JwtService jwtService;

    public AuthController(CredentialRepository credentialRepository, JwtService jwtService) {
        this.credentialRepository = credentialRepository;
        this.jwtService = jwtService;
    }

    @PostMapping("/token")
    public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest request) {
        if (request.getUsername() == null || request.getPassword() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        Optional<Credential> credentialOpt = credentialRepository.findByUsername(request.getUsername());
        if (credentialOpt.isEmpty()) {
            logger.warn("Failed login attempt for unknown user {}", request.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Credential credential = credentialOpt.get();
        if (!credential.getPassword().equals(request.getPassword())) {
            logger.warn("Failed login attempt for user {} due to invalid password", request.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = jwtService.generateToken(credential.getUsername(), credential.getRoles());
        TokenResponse response = new TokenResponse(token, jwtService.getExpirationSeconds());
        return ResponseEntity.ok(response);
    }
}
