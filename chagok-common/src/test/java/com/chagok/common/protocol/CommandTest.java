package com.chagok.common.protocol;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandTest {

    @Test
    void reportDefinesTwentyNineCommands() {
        assertEquals(29, Command.values().length);
    }

    @Test
    void testStartAliasAndUnknownCommand() {
        assertEquals(Command.TEST_START, Command.parse("TEST_START"));
        assertEquals(Command.TEST_START, Command.parse("AUTH_TEST_LOGIN"));
        assertNull(Command.parse("HELLO"));
        assertTrue(Command.TX_ADD.requiresAuth());
        assertTrue(!Command.PING.requiresAuth());
        assertTrue(!Command.TEST_START.requiresAuth());
    }

    @Test
    void malformedJsonBecomesRequestWithSniffedId() {
        assertEquals(8, JsonUtil.sniffReqId("{\"reqId\":8,\"cmd\":"));
        Request request = JsonUtil.parseRequest("{\"reqId\":1,\"cmd\":\"HELLO\",\"data\":{}}");
        assertNull(request.getCmd());
        assertEquals("HELLO", request.getCmdRaw());
    }
}
