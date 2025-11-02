package com.example.gateway.controller;

import com.example.gateway.config.GatewayProperties;
import com.example.gateway.model.DeviceSummary;
import com.example.gateway.model.LoginRequest;
import com.example.gateway.model.TokenResponse;
import com.example.gateway.model.UserSummary;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@RestController
@RequestMapping("/gateway")
public class GatewayController {

    private final RestTemplate restTemplate;
    private final GatewayProperties properties;

    public GatewayController(RestTemplate restTemplate, GatewayProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest request) {
        String url = properties.getAuthorization().getUrl() + "/auth/token";
        return exchange(url, HttpMethod.POST, request, TokenResponse.class);
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserSummary>> getUsers() {
        String url = properties.getUser().getUrl() + "/users";
        return exchange(url, HttpMethod.GET, null, new ParameterizedTypeReference<>() {});
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<UserSummary> getUser(@PathVariable Integer id) {
        String url = properties.getUser().getUrl() + "/users/" + id;
        return exchange(url, HttpMethod.GET, null, UserSummary.class);
    }

    @PostMapping("/users")
    public ResponseEntity<UserSummary> createUser(@RequestBody UserSummary user) {
        String url = properties.getUser().getUrl() + "/users";
        return exchange(url, HttpMethod.POST, user, UserSummary.class);
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<UserSummary> updateUser(@PathVariable Integer id, @RequestBody UserSummary user) {
        String url = properties.getUser().getUrl() + "/users/" + id;
        return exchange(url, HttpMethod.PUT, user, UserSummary.class);
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Integer id) {
        String url = properties.getUser().getUrl() + "/users/" + id;
        return exchangeDelete(url);
    }

    @GetMapping("/devices")
    public ResponseEntity<List<DeviceSummary>> getDevices() {
        String url = properties.getDevice().getUrl() + "/devices";
        return exchange(url, HttpMethod.GET, null, new ParameterizedTypeReference<>() {});
    }

    @GetMapping("/devices/{id}")
    public ResponseEntity<DeviceSummary> getDevice(@PathVariable Integer id) {
        String url = properties.getDevice().getUrl() + "/devices/" + id;
        return exchange(url, HttpMethod.GET, null, DeviceSummary.class);
    }

    @PostMapping("/devices")
    public ResponseEntity<DeviceSummary> createDevice(@RequestBody DeviceSummary device) {
        String url = properties.getDevice().getUrl() + "/devices";
        return exchange(url, HttpMethod.POST, device, DeviceSummary.class);
    }

    @PutMapping("/devices/{id}")
    public ResponseEntity<DeviceSummary> updateDevice(@PathVariable Integer id, @RequestBody DeviceSummary device) {
        String url = properties.getDevice().getUrl() + "/devices/" + id;
        return exchange(url, HttpMethod.PUT, device, DeviceSummary.class);
    }

    @DeleteMapping("/devices/{id}")
    public ResponseEntity<Void> deleteDevice(@PathVariable Integer id) {
        String url = properties.getDevice().getUrl() + "/devices/" + id;
        return exchangeDelete(url);
    }

    private <T> ResponseEntity<T> exchange(String url, HttpMethod method, Object body, Class<T> responseType) {
        try {
            HttpEntity<Object> requestEntity = body == null ? HttpEntity.EMPTY : new HttpEntity<>(body);
            ResponseEntity<T> response = restTemplate.exchange(url, method, requestEntity, responseType);
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (RestClientResponseException ex) {
            return ResponseEntity.status(ex.getStatusCode()).build();
        }
    }

    private <T> ResponseEntity<T> exchange(String url, HttpMethod method, Object body, ParameterizedTypeReference<T> typeReference) {
        try {
            HttpEntity<Object> requestEntity = body == null ? HttpEntity.EMPTY : new HttpEntity<>(body);
            ResponseEntity<T> response = restTemplate.exchange(url, method, requestEntity, typeReference);
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (RestClientResponseException ex) {
            return ResponseEntity.status(ex.getStatusCode()).build();
        }
    }

    private ResponseEntity<Void> exchangeDelete(String url) {
        try {
            restTemplate.delete(url);
            return ResponseEntity.noContent().build();
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            return ResponseEntity.status(ex.getStatusCode()).build();
        }
    }
}
