package com.chagok.server.service;

import com.chagok.common.dto.UserDTO;
import com.chagok.common.protocol.BizException;
import com.chagok.common.protocol.ErrorCode;
import com.chagok.server.ClientSession;
import com.chagok.server.dao.UserDAO;
import com.chagok.server.util.DummyDataLoader;
import com.chagok.server.util.ServerConfig;

import java.util.LinkedHashMap;
import java.util.Map;

public class TestAccountService {
    private final UserDAO userDAO;
    private final DummyDataLoader dummyDataLoader;

    public TestAccountService(UserDAO userDAO, DummyDataLoader dummyDataLoader) {
        this.userDAO = userDAO;
        this.dummyDataLoader = dummyDataLoader;
    }

    public Map<String, Object> start(ClientSession session) {
        ServerConfig config = ServerConfig.get();
        if (!config.testMode()) {
            throw new BizException(ErrorCode.E204);
        }
        UserDTO user = userDAO.findByLoginId(config.testLoginId());
        if (user == null) {
            throw new BizException(ErrorCode.E501, "테스트 계정(" + config.testLoginId() + ")을 찾을 수 없습니다");
        }
        session.setUser(user);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("user", user);
        body.put("testMode", true);
        body.put("recurringCreated", 0);
        return body;
    }

    public Map<String, Object> reset(ClientSession session) {
        ServerConfig config = ServerConfig.get();
        if (!config.testMode()) {
            throw new BizException(ErrorCode.E204);
        }
        if (!session.isLoggedIn() || !config.testLoginId().equals(session.getUser().getLoginId())) {
            throw new BizException(ErrorCode.E302, "테스트 계정만 데이터를 초기화할 수 있습니다");
        }
        int[] counts = dummyDataLoader.reset();
        UserDTO user = userDAO.findByLoginId(config.testLoginId());
        session.setUser(user);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("resetUsers", counts[0]);
        body.put("resetTx", counts[1]);
        return body;
    }
}
