INSERT INTO devices (name, type, status, max_consumption)
VALUES
    ('Thermostat', 'CLIMATE', 'ACTIVE', 1500),
    ('Solar Inverter', 'ENERGY', 'STANDBY', 5000)
ON CONFLICT (name) DO NOTHING;
