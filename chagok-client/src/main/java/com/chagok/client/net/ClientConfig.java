package com.chagok.client.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class ClientConfig {
    private final String host;
    private final int port;
    private final int timeoutMs;

    public ClientConfig(String host, int port, int timeoutMs) {
        this.host = host;
        this.port = port;
        this.timeoutMs = timeoutMs;
    }

    public static ClientConfig load() {
        Properties properties = new Properties();
        try (InputStream in = ClientConfig.class.getClassLoader().getResourceAsStream("client.properties")) {
            if (in != null) {
                properties.load(in);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("client.properties 를 읽지 못했습니다", ex);
        }
        Path local = Path.of("client.properties");
        if (Files.isRegularFile(local)) {
            try (Reader reader = Files.newBufferedReader(local, StandardCharsets.UTF_8)) {
                properties.load(reader);
            } catch (IOException ex) {
                throw new IllegalStateException("client.properties 를 읽지 못했습니다", ex);
            }
        }
        String host = env("CHAGOK_SERVER_HOST", properties.getProperty("server.host", "127.0.0.1"));
        int port = Integer.parseInt(env("CHAGOK_SERVER_PORT", properties.getProperty("server.port", "9000")));
        int timeout = Integer.parseInt(properties.getProperty("client.timeoutMs", "10000"));
        return new ClientConfig(host, port, timeout);
    }

    private static String env(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    public String host() {
        return host;
    }

    public int port() {
        return port;
    }

    public int timeoutMs() {
        return timeoutMs;
    }
}
