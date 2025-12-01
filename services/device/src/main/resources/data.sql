INSERT INTO devices (name, type, status, max_consumption, user_id)
VALUES
    ('Thermostat', 'CLIMATE', 'ACTIVE', 1500, 1),
    ('Solar Inverter', 'ENERGY', 'STANDBY', 5000, 2)
ON CONFLICT (name) DO NOTHING;
