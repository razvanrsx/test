INSERT INTO credentials (username, password, role)
VALUES ('admin', 'admin123', 'ADMIN')
ON CONFLICT (username) DO NOTHING;
