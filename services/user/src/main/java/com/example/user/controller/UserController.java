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
    public ResponseEntity<User> getById(@PathVariable Integer id) {
        Optional<User> user = userRepository.findById(id);
        return user.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<User> create(@RequestBody User user) {
        if (!StringUtils.hasText(user.getUsername()) || !StringUtils.hasText(user.getPassword())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        user.setId(null);
        User saved = userRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> update(@PathVariable Integer id, @RequestBody User user) {
        Optional<User> existingUser = userRepository.findById(id);
        if (existingUser.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (!StringUtils.hasText(user.getUsername())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        if (!StringUtils.hasText(user.getPassword())) {
            user.setPassword(existingUser.get().getPassword());
        }
        user.setId(id);
        User saved = userRepository.save(user);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        boolean removed = userRepository.delete(id);
        if (!removed) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }
}
