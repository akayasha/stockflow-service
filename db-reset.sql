-- StockFlow - reset script for local development.
-- Drops the stockflow database if it exists and recreates it from scratch.
-- Run this once if your previous attempt left the schema in an inconsistent state.

DROP DATABASE IF EXISTS stockflow;
CREATE DATABASE stockflow OWNER stockflow ENCODING 'UTF8';
GRANT ALL PRIVILEGES ON DATABASE stockflow TO stockflow;