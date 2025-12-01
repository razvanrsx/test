package com.example.monitoring.controller;

import com.example.monitoring.model.HourlyConsumption;
import com.example.monitoring.service.ConsumptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/monitoring")
public class MonitoringController {

    private final ConsumptionService consumptionService;

    public MonitoringController(ConsumptionService consumptionService) {
        this.consumptionService = consumptionService;
    }

    @Operation(summary = "List hourly consumption for all devices",
            responses = {@ApiResponse(responseCode = "200", description = "Aggregated consumption records")})
    @GetMapping("/consumption")
    public ResponseEntity<List<HourlyConsumption>> listAll() {
        return ResponseEntity.ok(consumptionService.findAll());
    }

    @Operation(summary = "List hourly consumption for a device",
            responses = {@ApiResponse(responseCode = "200", description = "Aggregated consumption records"),
                    @ApiResponse(responseCode = "404", description = "No records", content = @Content)})
    @GetMapping("/consumption/device/{deviceId}")
    public ResponseEntity<List<HourlyConsumption>> findByDevice(@PathVariable Long deviceId) {
        List<HourlyConsumption> results = consumptionService.findByDevice(deviceId);
        if (results.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(results);
    }
}
