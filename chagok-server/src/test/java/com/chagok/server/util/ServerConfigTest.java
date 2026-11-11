package com.chagok.server.util;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerConfigTest {

    @Test
    void envHostPortUserPasswordOverrideTheFile() {
        Properties file = new Properties();
        file.setProperty("db.host", "127.0.0.1");
        file.setProperty("db.port", "3306");
        file.setProperty("db.name", "chagok");
        file.setProperty("db.user", "file_user");
        file.setProperty("db.password", "file_secret");
        file.setProperty("db.ssl", "false");
        ServerConfig config = ServerConfig.resolve(file, Map.of(
                "CHAGOK_DB_HOST", "mydb.rds.amazonaws.com",
                "CHAGOK_DB_PORT", "3306",
                "CHAGOK_DB_USER", "chagok_app",
                "CHAGOK_DB_PASSWORD", "from-env",
                "CHAGOK_DB_SSL", "true"));
        assertEquals("mydb.rds.amazonaws.com", config.dbHost());
        assertEquals(3306, config.dbPort());
        assertEquals("chagok_app", config.dbUser());
        assertEquals("from-env", config.dbPassword());
        assertTrue(config.dbSsl());
        assertTrue(config.dbUrl().startsWith("jdbc:mysql://mydb.rds.amazonaws.com:3306/chagok?"));
        assertTrue(config.dbUrl().contains("useSSL=true"));
        assertFalse(config.dbUrl().contains("allowPublicKeyRetrieval=true"));
    }

    @Test
    void placeholderPasswordIsRejected() {
        Properties file = new Properties();
        file.setProperty("db.user", "chagok_app");
        file.setProperty("db.password", "change-me");
        assertThrows(IllegalStateException.class, () -> ServerConfig.resolve(file, Map.of()));
    }
}
