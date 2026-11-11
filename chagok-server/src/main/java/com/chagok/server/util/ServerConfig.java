package com.chagok.server.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

public final class ServerConfig {
    private static ServerConfig current;

    private final int port;
    private final int maxClients;
    private final String dbHost;
    private final int dbPort;
    private final String dbName;
    private final String dbUser;
    private final String dbPassword;
    private final boolean dbSsl;
    private final String dbUrl;
    private final boolean testMode;
    private final String testLoginId;

    private ServerConfig(int port, int maxClients, String dbHost, int dbPort, String dbName,
                         String dbUser, String dbPassword, boolean dbSsl, String dbUrl,
                         boolean testMode, String testLoginId) {
        this.port = port;
        this.maxClients = maxClients;
        this.dbHost = dbHost;
        this.dbPort = dbPort;
        this.dbName = dbName;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
        this.dbSsl = dbSsl;
        this.dbUrl = dbUrl;
        this.testMode = testMode;
        this.testLoginId = testLoginId;
    }

    public static ServerConfig get() {
        if (current == null) {
            throw new IllegalStateException("서버 설정이 로드되지 않았습니다");
        }
        return current;
    }

    public static void install(ServerConfig config) {
        current = config;
    }

    public static ServerConfig load() {
        Properties file = readFile();
        ServerConfig config = resolve(file, System.getenv());
        install(config);
        return config;
    }

    public static ServerConfig resolve(Properties file, Map<String, String> env) {
        int port = parseInt(first(env, "CHAGOK_SERVER_PORT", file.getProperty("server.port"), "9000"), "server.port");
        int maxClients = parseInt(file.getProperty("server.maxClients", "20"), "server.maxClients");
        String host = first(env, "CHAGOK_DB_HOST", file.getProperty("db.host"), "127.0.0.1");
        int dbPort = parseInt(first(env, "CHAGOK_DB_PORT", file.getProperty("db.port"), "3306"), "db.port");
        String name = first(env, "CHAGOK_DB_NAME", file.getProperty("db.name"), "chagok");
        String user = first(env, "CHAGOK_DB_USER", file.getProperty("db.user"), "");
        String password = first(env, "CHAGOK_DB_PASSWORD", file.getProperty("db.password"), "");
        boolean ssl = Boolean.parseBoolean(first(env, "CHAGOK_DB_SSL", file.getProperty("db.ssl"), "false"));
        boolean hostChosen = present(env, "CHAGOK_DB_HOST") || presentValue(file.getProperty("db.host"));
        String urlOverride = firstPresent(env.get("CHAGOK_DB_URL"), hostChosen ? null : file.getProperty("db.url"));
        String url = urlOverride != null ? urlOverride : jdbcUrl(host, dbPort, name, ssl);
        boolean testMode = Boolean.parseBoolean(first(env, "CHAGOK_TEST_MODE", file.getProperty("test.mode"), "true"));
        String loginId = first(env, "CHAGOK_TEST_LOGIN_ID", file.getProperty("test.login_id"), "test01");
        if (user.isBlank() || isPlaceholder(password)) {
            throw new IllegalStateException("""
                    DB 접속 정보가 없습니다. server.properties 또는 환경변수로 설정하세요.
                    CHAGOK_DB_HOST, CHAGOK_DB_PORT, CHAGOK_DB_USER, CHAGOK_DB_PASSWORD
                    예시 파일: chagok-server/src/main/resources/server.properties.sample""");
        }
        return new ServerConfig(port, maxClients, host, dbPort, name, user, password, ssl, url, testMode, loginId);
    }

    public static String jdbcUrl(String host, int port, String name, boolean ssl) {
        return "jdbc:mysql://" + host + ":" + port + "/" + name
                + "?serverTimezone=Asia/Seoul&characterEncoding=UTF-8&connectionTimeZone=Asia/Seoul"
                + "&useSSL=" + ssl
                + "&allowPublicKeyRetrieval=" + (!ssl);
    }

    private static Properties readFile() {
        String explicit = System.getProperty("chagok.server.config");
        if (explicit == null || explicit.isBlank()) {
            explicit = System.getenv("CHAGOK_SERVER_CONFIG");
        }
        Path path = null;
        if (explicit != null && !explicit.isBlank()) {
            path = Path.of(explicit);
        } else {
            for (String candidate : new String[]{"server.properties", "config/server.properties"}) {
                Path p = Path.of(candidate);
                if (Files.isRegularFile(p)) {
                    path = p;
                    break;
                }
            }
        }
        Properties properties = new Properties();
        if (path != null) {
            if (!Files.isRegularFile(path)) {
                throw new IllegalStateException("설정 파일을 찾을 수 없습니다: " + path);
            }
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                properties.load(reader);
            } catch (IOException ex) {
                throw new IllegalStateException("설정 파일을 읽지 못했습니다: " + path, ex);
            }
            return properties;
        }
        try (InputStream in = ServerConfig.class.getClassLoader().getResourceAsStream("server.properties")) {
            if (in != null) {
                properties.load(in);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("classpath server.properties 를 읽지 못했습니다", ex);
        }
        return properties;
    }

    private static boolean isPlaceholder(String password) {
        if (password == null || password.isBlank()) {
            return true;
        }
        String value = password.trim().toLowerCase(Locale.ROOT);
        return value.equals("change-me") || value.equals("********") || value.equals("your_password");
    }

    private static String first(Map<String, String> env, String key, String fileValue, String fallback) {
        if (present(env, key)) {
            return env.get(key).trim();
        }
        if (presentValue(fileValue)) {
            return fileValue.trim();
        }
        return fallback;
    }

    private static String firstPresent(String envValue, String fileValue) {
        if (presentValue(envValue)) {
            return envValue.trim();
        }
        if (presentValue(fileValue)) {
            return fileValue.trim();
        }
        return null;
    }

    private static boolean present(Map<String, String> env, String key) {
        return env != null && presentValue(env.get(key));
    }

    private static boolean presentValue(String value) {
        return value != null && !value.isBlank();
    }

    private static int parseInt(String value, String name) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalStateException(name + " 값이 숫자가 아닙니다: " + value);
        }
    }

    public int port() {
        return port;
    }

    public int maxClients() {
        return maxClients;
    }

    public String dbHost() {
        return dbHost;
    }

    public int dbPort() {
        return dbPort;
    }

    public String dbName() {
        return dbName;
    }

    public String dbUser() {
        return dbUser;
    }

    public String dbPassword() {
        return dbPassword;
    }

    public boolean dbSsl() {
        return dbSsl;
    }

    public String dbUrl() {
        return dbUrl;
    }

    public boolean testMode() {
        return testMode;
    }

    public String testLoginId() {
        return testLoginId;
    }
}
