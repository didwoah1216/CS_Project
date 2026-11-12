package com.chagok.common.protocol;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class JsonUtil {
    private static final Pattern REQ_ID = Pattern.compile("\"reqId\"\\s*:\\s*(\\d+)");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .serializeNulls()
            .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
            .create();

    private JsonUtil() {
    }

    public static String toJson(Object value) {
        return GSON.toJson(value);
    }

    public static JsonElement toTree(Object value) {
        return GSON.toJsonTree(value);
    }

    public static <T> T fromJson(String json, Class<T> type) {
        return GSON.fromJson(json, type);
    }

    public static <T> T fromJson(JsonElement json, Class<T> type) {
        return GSON.fromJson(json, type);
    }

    public static <T> T fromJson(JsonElement json, Type type) {
        return GSON.fromJson(json, type);
    }

    public static Request parseRequest(String line) {
        JsonElement element;
        try {
            element = JsonParser.parseString(line);
        } catch (JsonParseException ex) {
            throw new JsonSyntaxException(ex);
        }
        if (!element.isJsonObject()) {
            throw new JsonSyntaxException("요청은 JSON 객체여야 합니다");
        }
        JsonObject obj = element.getAsJsonObject();
        Request request = new Request();
        if (obj.has("reqId") && obj.get("reqId").isJsonPrimitive() && obj.get("reqId").getAsJsonPrimitive().isNumber()) {
            request.setReqId(obj.get("reqId").getAsLong());
        }
        if (obj.has("cmd") && !obj.get("cmd").isJsonNull()) {
            String raw = obj.get("cmd").getAsString();
            request.setCmdRaw(raw);
            request.setCmd(Command.parse(raw));
        }
        if (obj.has("data") && !obj.get("data").isJsonNull()) {
            if (!obj.get("data").isJsonObject()) {
                throw new JsonSyntaxException("data 는 JSON 객체여야 합니다");
            }
            request.setData(obj.getAsJsonObject("data"));
        } else {
            request.setData(new JsonObject());
        }
        return request;
    }

    public static long sniffReqId(String line) {
        if (line == null) {
            return 0;
        }
        Matcher matcher = REQ_ID.matcher(line);
        if (matcher.find()) {
            try {
                return Long.parseLong(matcher.group(1));
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    public static final class LocalDateAdapter implements com.google.gson.JsonSerializer<LocalDate>,
            com.google.gson.JsonDeserializer<LocalDate> {
        @Override
        public JsonElement serialize(LocalDate src, Type typeOfSrc, com.google.gson.JsonSerializationContext context) {
            return new JsonPrimitive(src.format(DATE));
        }

        @Override
        public LocalDate deserialize(JsonElement json, Type typeOfT, com.google.gson.JsonDeserializationContext context) {
            return LocalDate.parse(json.getAsString(), DATE);
        }
    }
}
