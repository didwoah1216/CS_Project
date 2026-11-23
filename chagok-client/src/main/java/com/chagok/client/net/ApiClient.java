package com.chagok.client.net;

import com.chagok.common.dto.BudgetDTO;
import com.chagok.common.dto.CategoryDTO;
import com.chagok.common.dto.CategoryStatDTO;
import com.chagok.common.dto.CompareDTO;
import com.chagok.common.dto.GoalDTO;
import com.chagok.common.dto.MonthlyStatDTO;
import com.chagok.common.dto.ScoreDTO;
import com.chagok.common.dto.WeekdayStatDTO;
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
            Command.SETTING_GET, Command.BUDGET_LIST, Command.RECURRING_RUN, Command.TX_SEARCH,
            Command.STAT_TREND, Command.FEEDBACK_SCORE, Command.GOAL_LIST, Command.STAT_WEEKDAY,
            Command.EXPORT_CSV);
    private static final Type CATEGORIES = new TypeToken<List<CategoryDTO>>() { }.getType();
    private static final Type BUDGETS = new TypeToken<List<BudgetDTO>>() { }.getType();
    private static final Type TRANSACTIONS = new TypeToken<List<TransactionDTO>>() { }.getType();
    private static final Type STATS = new TypeToken<List<CategoryStatDTO>>() { }.getType();
    private static final Type MONTHS = new TypeToken<List<MonthlyStatDTO>>() { }.getType();
    private static final Type GOALS = new TypeToken<List<GoalDTO>>() { }.getType();
    private static final Type WEEKDAYS = new TypeToken<List<WeekdayStatDTO>>() { }.getType();

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

    public JsonObject budgetList(String ym) {
        return call(Command.BUDGET_LIST, Map.of("ym", ym));
    }

    public List<BudgetDTO> budgets(JsonElement element) {
        return JsonUtil.fromJson(element, BUDGETS);
    }

    public BudgetDTO budgetSet(int categoryId, String ym, int amount) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("categoryId", categoryId);
        body.put("ym", ym);
        body.put("amount", amount);
        JsonObject data = call(Command.BUDGET_SET, body);
        return JsonUtil.fromJson(data.get("budget"), BudgetDTO.class);
    }

    public void budgetDelete(int budgetId) {
        call(Command.BUDGET_DELETE, Map.of("budgetId", budgetId));
    }

    public JsonObject budgetCopy(String ym) {
        return call(Command.BUDGET_COPY, Map.of("ym", ym));
    }

    public JsonObject recurringRun(String ym) {
        return call(Command.RECURRING_RUN, Map.of("ym", ym));
    }

    public JsonObject search(Map<String, Object> fields) {
        return call(Command.TX_SEARCH, fields);
    }

    public JsonObject trend(String ym) {
        return call(Command.STAT_TREND, Map.of("ym", ym));
    }

    public List<MonthlyStatDTO> months(JsonElement element) {
        return JsonUtil.fromJson(element, MONTHS);
    }

    public ScoreDTO score(String ym, boolean save) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ym", ym);
        body.put("save", save);
        JsonObject data = call(Command.FEEDBACK_SCORE, body);
        return JsonUtil.fromJson(data.get("score"), ScoreDTO.class);
    }

    public JsonObject goalList() {
        return call(Command.GOAL_LIST, Map.of());
    }

    public List<GoalDTO> goals(JsonElement element) {
        return JsonUtil.fromJson(element, GOALS);
    }

    public GoalDTO goalAdd(String name, int targetAmount, String startDate, String targetDate) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("goalName", name);
        body.put("targetAmount", targetAmount);
        body.put("startDate", startDate);
        body.put("targetDate", targetDate);
        return JsonUtil.fromJson(call(Command.GOAL_ADD, body).get("goal"), GoalDTO.class);
    }

    public GoalDTO goalDeposit(int goalId, int amount, String depositDate, String memo) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("goalId", goalId);
        body.put("amount", amount);
        body.put("depositDate", depositDate);
        if (memo != null && !memo.isBlank()) {
            body.put("memo", memo);
        }
        return JsonUtil.fromJson(call(Command.GOAL_DEPOSIT, body).get("goal"), GoalDTO.class);
    }

    public void goalDelete(int goalId) {
        call(Command.GOAL_DELETE, Map.of("goalId", goalId));
    }

    public JsonObject weekday(String ym) {
        return call(Command.STAT_WEEKDAY, Map.of("ym", ym));
    }

    public JsonObject weekday(String from, String to) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("from", from);
        body.put("to", to);
        return call(Command.STAT_WEEKDAY, body);
    }

    public List<WeekdayStatDTO> weekdays(JsonElement element) {
        return JsonUtil.fromJson(element, WEEKDAYS);
    }

    public JsonObject exportCsv(Map<String, Object> fields) {
        return call(Command.EXPORT_CSV, fields);
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
