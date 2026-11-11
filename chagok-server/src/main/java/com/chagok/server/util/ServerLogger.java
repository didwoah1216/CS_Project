package com.chagok.server.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class ServerLogger {
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

    private ServerLogger() {
    }

    public static void info(String thread, String cmd, int userId, String code, long ms) {
        write("INFO ", thread, cmd, "user=" + userId + " code=" + code + " " + ms + "ms", null);
    }

    public static void warn(String thread, String cmd, int userId, String message) {
        write("WARN ", thread, cmd, "user=" + userId + " " + message, null);
    }

    public static void error(String thread, String cmd, int userId, String code, Throwable error) {
        write("ERROR", thread, cmd, "user=" + userId + " code=" + code + " " + error, error);
    }

    private static void write(String level, String thread, String cmd, String message, Throwable error) {
        String line = "[" + LocalDateTime.now(com.chagok.common.util.DateUtil.KST).format(TS) + "]"
                + "[" + level + "][" + thread + "][" + (cmd == null ? "-" : cmd) + "] " + message;
        System.out.println(line);
        if (error != null) {
            error.printStackTrace(System.out);
        }
        try {
            Path dir = Path.of("logs");
            Files.createDirectories(dir);
            String name = "server-" + LocalDateTime.now(com.chagok.common.util.DateUtil.KST).format(DAY) + ".log";
            Files.writeString(dir.resolve(name), line + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
            // 콘솔 로그는 이미 남겼다.
        }
    }
}
