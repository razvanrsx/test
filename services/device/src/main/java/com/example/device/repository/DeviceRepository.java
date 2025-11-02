package com.example.device.repository;

import com.example.device.model.Device;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Repository
public class DeviceRepository {
    private static final Logger logger = LoggerFactory.getLogger(DeviceRepository.class);

    private final ObjectMapper objectMapper;
    private final Resource dataResource;
    private final Map<Integer, Device> devices = new ConcurrentHashMap<>();
    private final AtomicInteger sequence = new AtomicInteger();

    public DeviceRepository(ObjectMapper objectMapper,
                            @Value("classpath:data/devices.json") Resource dataResource) {
        this.objectMapper = objectMapper;
        this.dataResource = dataResource;
    }

    @PostConstruct
    void load() {
        try (InputStream inputStream = dataResource.getInputStream()) {
            List<Device> deviceList = objectMapper.readValue(inputStream, new TypeReference<>() {});
            deviceList.forEach(device -> {
                devices.put(device.getId(), device);
                sequence.updateAndGet(current -> Math.max(current, device.getId()));
            });
            logger.info("Loaded {} devices", devices.size());
        } catch (IOException e) {
            logger.warn("Unable to load device data", e);
        }
    }

    public List<Device> findAll() {
        Collection<Device> values = devices.values();
        return new ArrayList<>(values);
    }

    public Optional<Device> findById(Integer id) {
        return Optional.ofNullable(devices.get(id));
    }

    public Device save(Device device) {
        if (device.getId() == null) {
            device.setId(sequence.incrementAndGet());
        }
        devices.put(device.getId(), device);
        return device;
    }

    public boolean delete(Integer id) {
        return devices.remove(id) != null;
    }
}
