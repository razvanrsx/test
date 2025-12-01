INSERT INTO users (username, password, email, role)
VALUES
    ('admin', 'admin123', 'admin@example.com', 'ADMIN'),
    ('client', 'client123', 'client@example.com', 'CLIENT')
ON CONFLICT (username) DO NOTHING;
