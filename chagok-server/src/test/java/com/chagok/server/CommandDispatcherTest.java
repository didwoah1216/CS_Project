package com.chagok.server;

import com.chagok.common.protocol.JsonUtil;
import com.chagok.common.protocol.Request;
import com.chagok.common.protocol.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommandDispatcherTest {
    private final CommandDispatcher dispatcher = new CommandDispatcher(
            null, null, null, null, null, null, null, null, null, null, null);

    @Test
    void unknownCommandIsE101() {
        Response response = dispatcher.dispatch(request("{\"reqId\":1,\"cmd\":\"HELLO\"}"), new ClientSession());
        assertEquals("E101", response.getCode());
        assertEquals("ERROR", response.getStatus());
        assertEquals(1, response.getReqId());
    }

    @Test
    void authenticatedCommandWithoutSessionIsE203() {
        Response response = dispatcher.dispatch(
                request("{\"reqId\":2,\"cmd\":\"TX_ADD\",\"data\":{}}"), new ClientSession());
        assertEquals("E203", response.getCode());
    }

    @Test
    void authTestLoginAliasIsTestStartAndNeedsServices() {
        Request request = JsonUtil.parseRequest("{\"reqId\":4,\"cmd\":\"AUTH_TEST_LOGIN\",\"data\":{}}");
        assertEquals(com.chagok.common.protocol.Command.TEST_START, request.getCmd());
    }

    private static Request request(String json) {
        return JsonUtil.parseRequest(json);
    }
}
