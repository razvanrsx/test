package com.example.user.controller;

import com.example.user.model.User;
import com.example.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<User> getAll() {
        return userRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getById(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public ResponseEntity<User> findByUsername(@RequestParam String username) {
        if (!StringUtils.hasText(username)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        return userRepository.findByUsernameIgnoreCase(username)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PostMapping
    public ResponseEntity<User> create(@RequestBody User user) {
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
    public ResponseEntity<User> update(@PathVariable Long id, @RequestBody User user) {
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
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!userRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        userRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
