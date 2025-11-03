package com.example.authorization.controller;

import com.example.authorization.model.Credential;
import com.example.authorization.model.LoginRequest;
import com.example.authorization.model.TokenResponse;
import com.example.authorization.model.UserRecord;
import com.example.authorization.repository.CredentialRepository;
import com.example.authorization.service.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "JWT token issuance")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final CredentialRepository credentialRepository;
    private final JwtService jwtService;
    private final RestTemplate restTemplate;
    private final String userServiceUrl;

    public AuthController(CredentialRepository credentialRepository,
                          JwtService jwtService,
                          RestTemplate restTemplate,
                          @Value("${services.user.url:http://user-service:8081}") String userServiceUrl) {
        this.credentialRepository = credentialRepository;
        this.jwtService = jwtService;
        this.restTemplate = restTemplate;
        this.userServiceUrl = userServiceUrl;
    }

    @PostMapping("/token")
    @Operation(
            summary = "Authenticate user",
            description = "Validates credentials and returns a signed JWT token.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Token issued",
                            content = @Content(schema = @Schema(implementation = TokenResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Missing credentials"),
                    @ApiResponse(responseCode = "401", description = "Invalid username or password")
            }
    )
    public ResponseEntity<TokenResponse> login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Login request",
                    required = true,
                    content = @Content(schema = @Schema(implementation = LoginRequest.class))
            )
            @RequestBody LoginRequest request) {
        if (request.getUsername() == null || request.getPassword() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        Optional<Credential> credentialOpt = credentialRepository.findByUsernameIgnoreCase(request.getUsername());
        if (credentialOpt.isEmpty()) {
            Optional<UserRecord> userRecord = fetchUserRecord(request.getUsername());
            if (userRecord.isEmpty()) {
                logger.warn("Failed login attempt for unknown user {}", request.getUsername());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            UserRecord user = userRecord.get();
            if (!request.getPassword().equals(user.getPassword())) {
                logger.warn("Failed login attempt for user {} due to invalid password", request.getUsername());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            List<String> roles = StringUtils.hasText(user.getRole())
                    ? Collections.singletonList(user.getRole())
                    : Collections.emptyList();
            String token = jwtService.generateToken(user.getUsername(), roles);
            TokenResponse response = new TokenResponse(token, jwtService.getExpirationSeconds());
            return ResponseEntity.ok(response);
        }

        Credential credential = credentialOpt.get();
        if (!credential.getPassword().equals(request.getPassword())) {
            logger.warn("Failed login attempt for user {} due to invalid password", request.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<String> roles = StringUtils.hasText(credential.getRole())
                ? Collections.singletonList(credential.getRole())
                : Collections.emptyList();
        String token = jwtService.generateToken(credential.getUsername(), roles);
        TokenResponse response = new TokenResponse(token, jwtService.getExpirationSeconds());
        return ResponseEntity.ok(response);
    }

    private Optional<UserRecord> fetchUserRecord(String username) {
        if (!StringUtils.hasText(username)) {
            return Optional.empty();
        }
        try {
            String url = UriComponentsBuilder.fromHttpUrl(userServiceUrl)
                    .path("/users/search")
                    .queryParam("username", username)
                    .build()
                    .encode()
                    .toUriString();
            ResponseEntity<UserRecord> response = restTemplate.getForEntity(url, UserRecord.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return Optional.of(response.getBody());
            }
        } catch (RestClientResponseException ex) {
            if (!ex.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                logger.error("User service responded with status {} while fetching user {}", ex.getStatusCode(), username);
            }
        } catch (RestClientException ex) {
            logger.error("Failed to contact user service for username {}", username, ex);
        }
        return Optional.empty();
    }
}
