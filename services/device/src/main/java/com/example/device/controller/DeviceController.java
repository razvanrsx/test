package com.example.device.controller;

import com.example.device.model.Device;
import com.example.device.repository.DeviceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/devices")
public class DeviceController {

    private final DeviceRepository deviceRepository;

    public DeviceController(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    @GetMapping
    public List<Device> getAll(@RequestParam(name = "userId", required = false) Long userId) {
        if (userId != null) {
            return deviceRepository.findByUserId(userId);
        }
        return deviceRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Device> getById(@PathVariable Long id) {
        Optional<Device> device = deviceRepository.findById(id);
        return device.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Device> create(@RequestBody Device device) {
        if (!isValid(device)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        device.setId(null);
        Device saved = deviceRepository.save(device);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Device> update(@PathVariable Long id, @RequestBody Device device) {
        Optional<Device> existingDevice = deviceRepository.findById(id);
        if (existingDevice.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (!isValid(device)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        Device toUpdate = existingDevice.get();
        toUpdate.setName(device.getName());
        toUpdate.setType(device.getType());
        toUpdate.setStatus(device.getStatus());
        toUpdate.setMaxConsumption(device.getMaxConsumption());
        toUpdate.setUserId(device.getUserId());
        Device saved = deviceRepository.save(toUpdate);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!deviceRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        deviceRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private boolean isValid(Device device) {
        return device != null
                && StringUtils.hasText(device.getName())
                && StringUtils.hasText(device.getType())
                && StringUtils.hasText(device.getStatus())
                && device.getMaxConsumption() != null
                && device.getUserId() != null
                && device.getUserId() > 0;
    }
}
