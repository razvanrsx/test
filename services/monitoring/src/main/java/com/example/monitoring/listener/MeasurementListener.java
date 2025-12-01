package com.example.monitoring.listener;

import com.example.monitoring.model.DeviceMeasurement;
import com.example.monitoring.service.ConsumptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class MeasurementListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(MeasurementListener.class);
    public static final String QUEUE_NAME = "device.measurements";

    private final ConsumptionService consumptionService;

    public MeasurementListener(ConsumptionService consumptionService) {
        this.consumptionService = consumptionService;
    }

    @RabbitListener(queues = QUEUE_NAME)
    public void receiveMeasurement(DeviceMeasurement measurement) {
        try {
            LOGGER.info("Received measurement for device {} at {}", measurement.getDeviceId(), measurement.getTimestamp());
            consumptionService.recordMeasurement(measurement);
        } catch (Exception ex) {
            LOGGER.error("Failed to persist measurement: {}", ex.getMessage(), ex);
        }
    }
}
