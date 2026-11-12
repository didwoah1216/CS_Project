package com.chagok.client.net;

import com.chagok.common.dto.CategoryDTO;
import com.chagok.common.dto.CategoryStatDTO;
import com.chagok.common.dto.CompareDTO;
import com.chagok.common.dto.MonthlyStatDTO;
import com.chagok.common.dto.SettingDTO;
import com.chagok.common.dto.TransactionDTO;
import com.chagok.common.dto.UserDTO;
import com.chagok.common.dto.WarningDTO;
import com.chagok.common.protocol.Command;
import com.chagok.common.protocol.ErrorCode;
import com.chagok.common.protocol.JsonUtil;
import com.chagok.common.protocol.Response;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ApiClient {
    private static final Set<Command> IDEMPOTENT = EnumSet.of(
            Command.PING, Command.TEST_START, Command.CATEGORY_LIST, Command.TX_LIST_MONTH, Command.TX_LIST_DAY,
            Command.STAT_CATEGORY, Command.STAT_MONTHLY, Command.FEEDBACK_WARNING, Command.COMPARE_AGE_GROUP,
            Command.SETTING_GET);
    private static final Type CATEGORIES = new TypeToken<List<CategoryDTO>>() { }.getType();
    private static final Type TRANSACTIONS = new TypeToken<List<TransactionDTO>>() { }.getType();
    private static final Type STATS = new TypeToken<List<CategoryStatDTO>>() { }.getType();

    private final ServerConnection connection;
    private long sequence;
    private boolean started;

    public ApiClient(ServerConnection connection) {
        this.connection = connection;
    }

    public void ping() {
        call(Command.PING, Map.of());
    }

    public UserDTO testStart() {
        JsonObject data = call(Command.TEST_START, Map.of());
        started = true;
        return JsonUtil.fromJson(data.get("user"), UserDTO.class);
    }

    public List<CategoryDTO> categories(String type) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (type != null) {
            body.put("type", type);
        }
        JsonObject data = call(Command.CATEGORY_LIST, body);
        return JsonUtil.fromJson(data.get("items"), CATEGORIES);
    }

    public TransactionDTO addTransaction(Map<String, Object> fields) {
        JsonObject data = call(Command.TX_ADD, fields);
        return JsonUtil.fromJson(data.get("transaction"), TransactionDTO.class);
    }

    public TransactionDTO updateTransaction(Map<String, Object> fields) {
        JsonObject data = call(Command.TX_UPDATE, fields);
        return JsonUtil.fromJson(data.get("transaction"), TransactionDTO.class);
    }

    public void deleteTransaction(int transactionId, boolean deleteAuto) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("transactionId", transactionId);
        body.put("deleteAuto", deleteAuto);
        call(Command.TX_DELETE, body);
    }

    public JsonObject listMonth(String ym) {
        return call(Command.TX_LIST_MONTH, Map.of("ym", ym));
    }

    public List<TransactionDTO> transactions(JsonElement element) {
        return JsonUtil.fromJson(element, TRANSACTIONS);
    }

    public WarningDTO warning(String ym) {
        JsonObject data = call(Command.FEEDBACK_WARNING, Map.of("ym", ym));
        return JsonUtil.fromJson(data.get("warning"), WarningDTO.class);
    }

    public JsonObject categoryStat(String ym) {
        return call(Command.STAT_CATEGORY, Map.of("ym", ym, "type", "EXPENSE"));
    }

    public List<CategoryStatDTO> statItems(JsonElement element) {
        return JsonUtil.fromJson(element, STATS);
    }

    public JsonObject monthly(String ym) {
        return call(Command.STAT_MONTHLY, Map.of("ym", ym));
    }

    public MonthlyStatDTO monthlyOf(JsonElement element) {
        return JsonUtil.fromJson(element, MonthlyStatDTO.class);
    }

    public CompareDTO compare(String ym, boolean sameGender) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ym", ym);
        body.put("sameGender", sameGender);
        JsonObject data = call(Command.COMPARE_AGE_GROUP, body);
        return JsonUtil.fromJson(data.get("compare"), CompareDTO.class);
    }

    public SettingDTO setting() {
        JsonObject data = call(Command.SETTING_GET, Map.of());
        return JsonUtil.fromJson(data.get("setting"), SettingDTO.class);
    }

    public SettingDTO updateSetting(SettingDTO setting) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("warnEnabled", setting.isWarnEnabled());
        body.put("warnRatio", setting.getWarnRatio());
        body.put("peerGapRatio", setting.getPeerGapRatio());
        body.put("budgetAlertRatio", setting.getBudgetAlertRatio());
        body.put("compareSameGender", setting.isCompareSameGender());
        JsonObject data = call(Command.SETTING_UPDATE, body);
        return JsonUtil.fromJson(data.get("setting"), SettingDTO.class);
    }

    public JsonObject resetDummy() {
        return call(Command.DUMMY_RESET, Map.of());
    }

    private JsonObject call(Command command, Object data) {
        String payload = envelope(command.name(), data);
        try {
            return parse(connection.exchange(payload));
        } catch (ApiException ex) {
            if (ex.getCode() != ErrorCode.C901 && ex.getCode() != ErrorCode.C902) {
                throw ex;
            }
            connection.connect();
            if (started) {
                parse(connection.exchange(envelope(Command.TEST_START.name(), Map.of())));
            }
            if (IDEMPOTENT.contains(command)) {
                return parse(connection.exchange(payload));
            }
            throw ex;
        }
    }

    private String envelope(String command, Object data) {
        JsonObject root = new JsonObject();
        root.addProperty("reqId", ++sequence);
        root.addProperty("cmd", command);
        root.add("data", data == null ? new JsonObject() : JsonUtil.toTree(data));
        return JsonUtil.toJson(root);
    }

    private static JsonObject parse(String line) {
        Response response = JsonUtil.fromJson(line, Response.class);
        if (response == null || !"OK".equals(response.getStatus())) {
            ErrorCode code = ErrorCode.E500;
            String message = "서버 응답이 올바르지 않습니다";
            if (response != null && response.getCode() != null) {
                try {
                    code = ErrorCode.valueOf(response.getCode());
                } catch (IllegalArgumentException ignored) {
                    code = ErrorCode.E500;
                }
                message = response.getMessage();
            }
            throw new ApiException(code, message);
        }
        if (response.getData() == null || response.getData().isJsonNull()) {
            return new JsonObject();
        }
        return response.getData().getAsJsonObject();
    }
}
