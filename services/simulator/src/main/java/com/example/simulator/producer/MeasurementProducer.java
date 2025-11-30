package com.example.simulator.producer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Component
public class MeasurementProducer {
    private static final Logger LOGGER = LoggerFactory.getLogger(MeasurementProducer.class);
    public static final String QUEUE_NAME = "device.measurements";

    private final RabbitTemplate rabbitTemplate;
    private final List<Long> deviceIds;
    private final Random random = new Random();

    public MeasurementProducer(RabbitTemplate rabbitTemplate,
                               @Value("${simulator.deviceIds:1,2,3}") String deviceIds,
                               @Value("${simulator.baseConsumption:0.5}") double baseConsumption) {
        this.rabbitTemplate = rabbitTemplate;
        this.deviceIds = Arrays.stream(deviceIds.split(","))
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .map(Long::valueOf)
                .collect(Collectors.toList());
        this.baseConsumption = BigDecimal.valueOf(baseConsumption);
    }

    private final BigDecimal baseConsumption;

    @Scheduled(fixedDelayString = "${simulator.intervalMs:60000}")
    public void publishMeasurement() {
        if (deviceIds.isEmpty()) {
            LOGGER.warn("No device ids configured for simulator; skipping publish");
            return;
        }
        Long deviceId = deviceIds.get(random.nextInt(deviceIds.size()));
        BigDecimal variance = BigDecimal.valueOf(random.nextDouble(0.1)).setScale(3, RoundingMode.HALF_UP);
        BigDecimal reading = baseConsumption.add(variance);
        MeasurementEvent event = new MeasurementEvent(deviceId, reading, OffsetDateTime.now());
        rabbitTemplate.convertAndSend(QUEUE_NAME, event);
        LOGGER.info("Published measurement {} kWh for device {}", reading, deviceId);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void sendWarmupMeasurement() {
        LOGGER.info("Sending warm-up measurement so monitoring has data shortly after startup");
        publishMeasurement();
    }
}
