package com.example.user.controller;

import com.example.user.model.User;
import com.example.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/users")
@Tag(name = "Users", description = "User CRUD operations")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    @Operation(
            summary = "List users",
            description = "Returns all registered users.",
            responses = {@ApiResponse(responseCode = "200", description = "Users retrieved")}
    )
    public List<User> getAll() {
        return userRepository.findAll();
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get user by id",
            description = "Fetch a user by database identifier.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User found"),
                    @ApiResponse(responseCode = "404", description = "User not found")
            }
    )
    public ResponseEntity<User> getById(@Parameter(description = "User identifier") @PathVariable Long id) {
        return userRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    @Operation(
            summary = "Find user by username",
            description = "Returns a user record that matches the provided username (case insensitive).",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User found"),
                    @ApiResponse(responseCode = "400", description = "Missing username"),
                    @ApiResponse(responseCode = "404", description = "User not found")
            }
    )
    public ResponseEntity<User> findByUsername(
            @Parameter(description = "Username to search", required = true) @RequestParam String username) {
        if (!StringUtils.hasText(username)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        return userRepository.findByUsernameIgnoreCase(username)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PostMapping
    @Operation(
            summary = "Create user",
            description = "Creates a new platform user.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "User created"),
                    @ApiResponse(responseCode = "400", description = "Missing required fields"),
                    @ApiResponse(responseCode = "409", description = "Username already exists")
            }
    )
    public ResponseEntity<User> create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "User payload",
                    required = true,
                    content = @Content(schema = @Schema(implementation = User.class))
            )
            @RequestBody User user) {
        if (!StringUtils.hasText(user.getUsername()) || !StringUtils.hasText(user.getPassword())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        if (!StringUtils.hasText(user.getEmail()) || !StringUtils.hasText(user.getRole())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        if (userRepository.existsByUsernameIgnoreCase(user.getUsername())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        user.setId(null);
        User saved = userRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update user",
            description = "Updates mutable fields for an existing user.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User updated"),
                    @ApiResponse(responseCode = "400", description = "Missing required fields"),
                    @ApiResponse(responseCode = "404", description = "User not found"),
                    @ApiResponse(responseCode = "409", description = "Username already exists")
            }
    )
    public ResponseEntity<User> update(
            @Parameter(description = "User identifier") @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Updated user payload",
                    required = true,
                    content = @Content(schema = @Schema(implementation = User.class))
            )
            @RequestBody User user) {
        Optional<User> existingUser = userRepository.findById(id);
        if (existingUser.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (!StringUtils.hasText(user.getUsername())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        if (!StringUtils.hasText(user.getEmail()) || !StringUtils.hasText(user.getRole())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        User toUpdate = existingUser.get();
        Optional<User> duplicate = userRepository.findByUsernameIgnoreCase(user.getUsername());
        if (duplicate.isPresent() && !duplicate.get().getId().equals(id)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        toUpdate.setUsername(user.getUsername());
        toUpdate.setEmail(user.getEmail());
        toUpdate.setRole(user.getRole());
        if (StringUtils.hasText(user.getPassword())) {
            toUpdate.setPassword(user.getPassword());
        }
        User saved = userRepository.save(toUpdate);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete user",
            description = "Removes a user by identifier.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "User deleted"),
                    @ApiResponse(responseCode = "404", description = "User not found")
            }
    )
    public ResponseEntity<Void> delete(@Parameter(description = "User identifier") @PathVariable Long id) {
        if (!userRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        userRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
