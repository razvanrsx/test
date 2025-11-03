package com.example.device.controller;

import com.example.device.model.Device;
import com.example.device.repository.DeviceRepository;
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
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/devices")
@Tag(name = "Devices", description = "Device CRUD operations")
public class DeviceController {

    private final DeviceRepository deviceRepository;

    public DeviceController(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    @GetMapping
    @Operation(
            summary = "List devices",
            description = "Returns all devices or those filtered by owning user.",
            responses = {@ApiResponse(responseCode = "200", description = "Devices retrieved")}
    )
    public List<Device> getAll(
            @Parameter(description = "Optional user filter")
            @RequestParam(name = "userId", required = false) Long userId) {
        if (userId != null) {
            return deviceRepository.findByUserId(userId);
        }
        return deviceRepository.findAll();
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get device by id",
            description = "Fetch a device by identifier.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Device found"),
                    @ApiResponse(responseCode = "404", description = "Device not found")
            }
    )
    public ResponseEntity<Device> getById(@Parameter(description = "Device identifier") @PathVariable Long id) {
        Optional<Device> device = deviceRepository.findById(id);
        return device.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(
            summary = "Create device",
            description = "Registers a new device and assigns it to a user.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Device created"),
                    @ApiResponse(responseCode = "400", description = "Validation error"),
                    @ApiResponse(responseCode = "409", description = "Duplicate device name")
            }
    )
    public ResponseEntity<?> create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Device payload",
                    required = true,
                    content = @Content(schema = @Schema(implementation = Device.class))
            )
            @RequestBody Device device) {
        if (!isValid(device)) {
            return error(HttpStatus.BAD_REQUEST, "Name, type, status, max consumption, and user are required.");
        }

        if (deviceRepository.findByNameIgnoreCase(device.getName()).isPresent()) {
            return error(HttpStatus.CONFLICT, "A device with this name already exists. Choose a different name.");
        }

        device.setId(null);
        Device saved = deviceRepository.save(device);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update device",
            description = "Updates device attributes and ownership.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Device updated"),
                    @ApiResponse(responseCode = "400", description = "Validation error"),
                    @ApiResponse(responseCode = "404", description = "Device not found"),
                    @ApiResponse(responseCode = "409", description = "Duplicate device name")
            }
    )
    public ResponseEntity<?> update(
            @Parameter(description = "Device identifier") @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Updated device payload",
                    required = true,
                    content = @Content(schema = @Schema(implementation = Device.class))
            )
            @RequestBody Device device) {
        Optional<Device> existingDevice = deviceRepository.findById(id);
        if (existingDevice.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (!isValid(device)) {
            return error(HttpStatus.BAD_REQUEST, "Name, type, status, max consumption, and user are required.");
        }

        Optional<Device> duplicate = deviceRepository.findByNameIgnoreCase(device.getName());
        if (duplicate.isPresent() && !duplicate.get().getId().equals(id)) {
            return error(HttpStatus.CONFLICT, "A device with this name already exists. Choose a different name.");
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
    @Operation(
            summary = "Delete device",
            description = "Deletes a device by identifier.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Device deleted"),
                    @ApiResponse(responseCode = "404", description = "Device not found")
            }
    )
    public ResponseEntity<Void> delete(@Parameter(description = "Device identifier") @PathVariable Long id) {
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

    private ResponseEntity<Map<String, String>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("message", message));
    }
}
