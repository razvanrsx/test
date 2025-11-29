package com.example.simulator.producer;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class MeasurementEvent {
    private Long deviceId;
    private BigDecimal consumption;
    private OffsetDateTime timestamp;

    public MeasurementEvent(Long deviceId, BigDecimal consumption, OffsetDateTime timestamp) {
        this.deviceId = deviceId;
        this.consumption = consumption;
        this.timestamp = timestamp;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public BigDecimal getConsumption() {
        return consumption;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }
}
