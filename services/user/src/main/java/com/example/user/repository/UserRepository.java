package com.example.user.repository;

import com.example.user.model.User;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Repository
public class UserRepository {
    private static final Logger logger = LoggerFactory.getLogger(UserRepository.class);

    private final ObjectMapper objectMapper;
    private final Resource dataResource;
    private final Map<Integer, User> users = new ConcurrentHashMap<>();
    private final AtomicInteger sequence = new AtomicInteger();

    public UserRepository(ObjectMapper objectMapper,
                          @Value("classpath:data/users.json") Resource dataResource) {
        this.objectMapper = objectMapper;
        this.dataResource = dataResource;
    }

    @PostConstruct
    void load() {
        try (InputStream inputStream = dataResource.getInputStream()) {
            List<User> userList = objectMapper.readValue(inputStream, new TypeReference<>() {});
            userList.forEach(user -> {
                users.put(user.getId(), user);
                sequence.updateAndGet(current -> Math.max(current, user.getId()));
            });
            logger.info("Loaded {} users", users.size());
        } catch (IOException e) {
            logger.warn("Unable to load initial user data", e);
        }
    }

    public List<User> findAll() {
        Collection<User> values = users.values();
        return new ArrayList<>(values);
    }

    public Optional<User> findById(Integer id) {
        return Optional.ofNullable(users.get(id));
    }

    public User save(User user) {
        if (user.getId() == null) {
            user.setId(sequence.incrementAndGet());
        }
        users.put(user.getId(), user);
        return user;
    }

    public boolean delete(Integer id) {
        return users.remove(id) != null;
    }
}
