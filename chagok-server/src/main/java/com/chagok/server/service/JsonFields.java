package com.chagok.server.service;

import com.chagok.common.protocol.BizException;
import com.chagok.common.protocol.ErrorCode;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public final class JsonFields {
    private JsonFields() {
    }

    public static boolean present(JsonObject data, String field) {
        return data != null && data.has(field) && !data.get(field).isJsonNull();
    }

    public static String text(JsonObject data, String field) {
        if (!present(data, field)) {
            return null;
        }
        return data.get(field).getAsString();
    }

    public static Integer integer(JsonObject data, String field) {
        if (!present(data, field)) {
            return null;
        }
        JsonElement element = data.get(field);
        if (!element.isJsonPrimitive()) {
            throw new BizException(ErrorCode.E103, field + " 값이 올바르지 않습니다");
        }
        JsonPrimitive primitive = element.getAsJsonPrimitive();
        try {
            if (primitive.isNumber()) {
                double raw = primitive.getAsDouble();
                if (raw != Math.rint(raw) || raw > Integer.MAX_VALUE || raw < Integer.MIN_VALUE) {
                    throw new BizException(ErrorCode.E103, field + " 값이 올바르지 않습니다");
                }
                return primitive.getAsInt();
            }
            String text = primitive.getAsString().trim();
            if (!text.matches("-?\\d+")) {
                throw new BizException(ErrorCode.E103, field + " 값이 올바르지 않습니다");
            }
            long value = Long.parseLong(text);
            if (value > Integer.MAX_VALUE || value < Integer.MIN_VALUE) {
                throw new BizException(ErrorCode.E103, field + " 값이 올바르지 않습니다");
            }
            return (int) value;
        } catch (NumberFormatException ex) {
            throw new BizException(ErrorCode.E103, field + " 값이 올바르지 않습니다");
        }
    }

    public static boolean bool(JsonObject data, String field, boolean required) {
        if (!present(data, field)) {
            if (required) {
                throw new BizException(ErrorCode.E102, field + " 값이 필요합니다");
            }
            return false;
        }
        JsonElement element = data.get(field);
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isBoolean()) {
            throw new BizException(ErrorCode.E103, field + " 값이 올바르지 않습니다");
        }
        return element.getAsBoolean();
    }
}
