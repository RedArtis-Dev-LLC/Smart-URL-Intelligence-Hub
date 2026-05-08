-- Create per-service databases. Runs once on first container start
-- (any file in /docker-entrypoint-initdb.d is executed by the postgres image).
CREATE DATABASE auth_db;
CREATE DATABASE links_db;
CREATE DATABASE webhooks_db;
