package com.example.monitoring.repository;

import com.example.monitoring.model.HourlyConsumption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface HourlyConsumptionRepository extends JpaRepository<HourlyConsumption, Long> {
    Optional<HourlyConsumption> findByDeviceIdAndHourStart(Long deviceId, LocalDateTime hourStart);

    List<HourlyConsumption> findByDeviceIdOrderByHourStartDesc(Long deviceId);
}
