-- Migration script to create Users table for Steam authentication
-- Author: bageshwar.pn
-- Date: 2026

-- Create sequence for Users table
IF NOT EXISTS (SELECT * FROM sys.sequences WHERE name = 'users_seq')
BEGIN
    CREATE SEQUENCE users_seq
        START WITH 1
        INCREMENT BY 50
        CACHE 10;
END
GO

-- Create Users table
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Users')
BEGIN
    CREATE TABLE Users (
        id BIGINT NOT NULL PRIMARY KEY,
        steamId64 VARCHAR(20) NOT NULL UNIQUE,
        steamId3 VARCHAR(255) NOT NULL,
        personaName VARCHAR(255) NOT NULL,
        avatarUrl VARCHAR(500),
        avatarMediumUrl VARCHAR(500),
        avatarSmallUrl VARCHAR(500),
        profileUrl VARCHAR(500),
        accountCreated DATETIME2,
        vacBanned BIT NOT NULL DEFAULT 0,
        country VARCHAR(10),
        createdAt DATETIME2 NOT NULL,
        lastLogin DATETIME2 NOT NULL
    );
    
    -- Create indexes
    CREATE UNIQUE INDEX idx_users_steamid64 ON Users(steamId64);
    CREATE INDEX idx_users_steamid3 ON Users(steamId3);
END
GO
