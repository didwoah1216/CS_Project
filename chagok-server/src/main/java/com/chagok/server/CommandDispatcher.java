package com.chagok.server;

import com.chagok.common.protocol.BizException;
import com.chagok.common.protocol.Command;
import com.chagok.common.protocol.ErrorCode;
import com.chagok.common.protocol.Request;
import com.chagok.common.protocol.Response;
import com.chagok.common.util.DateUtil;
import com.chagok.server.service.CompareService;
import com.chagok.server.service.FeedbackService;
import com.chagok.server.service.JsonFields;
import com.chagok.server.service.SettingService;
import com.chagok.server.service.StatisticsService;
import com.chagok.server.service.TestAccountService;
import com.chagok.server.service.TransactionService;
import com.chagok.server.util.DbException;
import com.chagok.server.util.ServerLogger;
import com.google.gson.JsonObject;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public class CommandDispatcher {
    private static final Set<Command> PHASE1 = EnumSet.of(
            Command.PING, Command.TEST_START, Command.DUMMY_RESET,
            Command.CATEGORY_LIST, Command.TX_ADD, Command.TX_UPDATE, Command.TX_DELETE,
            Command.TX_LIST_MONTH, Command.TX_LIST_DAY,
            Command.STAT_CATEGORY, Command.STAT_MONTHLY,
            Command.FEEDBACK_WARNING, Command.COMPARE_AGE_GROUP,
            Command.SETTING_GET, Command.SETTING_UPDATE);

    private final TestAccountService testAccountService;
    private final TransactionService transactionService;
    private final StatisticsService statisticsService;
    private final FeedbackService feedbackService;
    private final CompareService compareService;
    private final SettingService settingService;
    private final com.chagok.server.dao.CategoryDAO categoryDAO;

    public CommandDispatcher(TestAccountService testAccountService, TransactionService transactionService,
                             StatisticsService statisticsService, FeedbackService feedbackService,
                             CompareService compareService, SettingService settingService,
                             com.chagok.server.dao.CategoryDAO categoryDAO) {
        this.testAccountService = testAccountService;
        this.transactionService = transactionService;
        this.statisticsService = statisticsService;
        this.feedbackService = feedbackService;
        this.compareService = compareService;
        this.settingService = settingService;
        this.categoryDAO = categoryDAO;
    }

    public Response dispatch(Request request, ClientSession session) {
        if (request == null || request.getCmd() == null) {
            return Response.error(request, ErrorCode.E101);
        }
        Command command = request.getCmd();
        if (command.requiresAuth() && (session == null || !session.isLoggedIn())) {
            return Response.error(request, ErrorCode.E203);
        }
        if (!PHASE1.contains(command)) {
            return Response.error(request, ErrorCode.E304, "이후 단계에서 제공되는 기능입니다");
        }
        try {
            Object data = switch (command) {
                case PING -> Map.of("serverTime", DateUtil.formatNow());
                case TEST_START -> testAccountService.start(session);
                case DUMMY_RESET -> testAccountService.reset(session);
                case CATEGORY_LIST -> Map.of("items", categoryDAO.findAll(blankToNull(request.getString("type"))));
                case TX_ADD -> transactionService.add(session.getUserId(), request.getData());
                case TX_UPDATE -> transactionService.update(session.getUserId(), request.getData());
                case TX_DELETE -> transactionService.delete(session.getUserId(), request.getData());
                case TX_LIST_MONTH -> transactionService.listMonth(session.getUserId(), requiredYm(request));
                case TX_LIST_DAY -> transactionService.listDay(session.getUserId(), requiredText(request, "date", "날짜"));
                case STAT_CATEGORY -> statisticsService.categoryStat(session.getUserId(), requiredYm(request),
                        request.getString("type"));
                case STAT_MONTHLY -> statisticsService.monthly(session.getUserId(), requiredYm(request));
                case FEEDBACK_WARNING -> Map.of("warning", feedbackService.evaluate(session, requiredYm(request)));
                case COMPARE_AGE_GROUP -> Map.of("compare", compareService.compare(
                        session.getUserId(), requiredYm(request), optionalBool(request.getData(), "sameGender")));
                case SETTING_GET -> settingService.get(session.getUserId());
                case SETTING_UPDATE -> settingService.update(session.getUserId(), request.getData());
                default -> throw new BizException(ErrorCode.E304, "이후 단계에서 제공되는 기능입니다");
            };
            String message = switch (command) {
                case TX_ADD, TX_UPDATE -> "저장 완료";
                case TX_DELETE -> "삭제 완료";
                case DUMMY_RESET -> "더미 데이터를 초기화했습니다";
                case SETTING_UPDATE -> "설정을 저장했습니다";
                default -> "";
            };
            return Response.ok(request, data, message);
        } catch (BizException ex) {
            ServerLogger.warn(Thread.currentThread().getName(), command.name(), userId(session),
                    ex.getCode().getCode() + " " + ex.getMessage());
            return Response.error(request, ex.getCode(), ex.getMessage());
        } catch (DbException ex) {
            ServerLogger.error(Thread.currentThread().getName(), command.name(), userId(session),
                    ErrorCode.E501.getCode(), ex);
            return Response.error(request, ErrorCode.E501);
        } catch (Exception ex) {
            ServerLogger.error(Thread.currentThread().getName(), command.name(), userId(session),
                    ErrorCode.E500.getCode(), ex);
            return Response.error(request, ErrorCode.E500);
        }
    }

    private static String requiredYm(Request request) {
        String ym = requiredText(request, "ym", "월(ym)");
        DateUtil.parseYm(ym);
        return ym;
    }

    private static String requiredText(Request request, String field, String label) {
        if (!JsonFields.present(request.getData(), field)) {
            throw new BizException(ErrorCode.E102, label + "이 필요합니다");
        }
        String value = request.getString(field);
        if (value == null || value.isBlank()) {
            throw new BizException(ErrorCode.E102, label + "이 필요합니다");
        }
        return value.trim();
    }

    private static Boolean optionalBool(JsonObject data, String field) {
        if (!JsonFields.present(data, field)) {
            return null;
        }
        return JsonFields.bool(data, field, true);
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static int userId(ClientSession session) {
        return session == null ? 0 : session.getUserId();
    }
}
