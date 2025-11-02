package com.example.device.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class DeviceExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceExceptionHandler.class);

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        LOGGER.warn("Constraint violation while processing device request", ex);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", "Device data violates database constraints. Ensure the name is unique and all required fields are provided."));
    }
}
