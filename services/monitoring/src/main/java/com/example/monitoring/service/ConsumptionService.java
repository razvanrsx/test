package com.example.monitoring.service;

import com.example.monitoring.model.DeviceMeasurement;
import com.example.monitoring.model.HourlyConsumption;
import com.example.monitoring.repository.HourlyConsumptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ConsumptionService {

    private final HourlyConsumptionRepository repository;

    public ConsumptionService(HourlyConsumptionRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public HourlyConsumption recordMeasurement(DeviceMeasurement measurement) {
        if (measurement.getDeviceId() == null || measurement.getConsumption() == null || measurement.getTimestamp() == null) {
            throw new IllegalArgumentException("Measurement must include deviceId, consumption, and timestamp");
        }
        LocalDateTime hourStart = measurement.getTimestamp().withMinute(0).withSecond(0).withNano(0).toLocalDateTime();
        BigDecimal value = measurement.getConsumption();

        HourlyConsumption existing = repository.findByDeviceIdAndHourStart(measurement.getDeviceId(), hourStart)
                .orElseGet(() -> {
                    HourlyConsumption created = new HourlyConsumption();
                    created.setDeviceId(measurement.getDeviceId());
                    created.setHourStart(hourStart);
                    created.setConsumption(BigDecimal.ZERO);
                    return created;
                });

        existing.setConsumption(existing.getConsumption().add(value));
        return repository.save(existing);
    }

    public List<HourlyConsumption> findByDevice(Long deviceId) {
        return repository.findByDeviceIdOrderByHourStartDesc(deviceId);
    }

    public List<HourlyConsumption> findAll() {
        return repository.findAll();
    }
}
