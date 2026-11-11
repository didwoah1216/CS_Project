package com.chagok.server;

import com.chagok.common.protocol.ErrorCode;
import com.chagok.common.protocol.JsonUtil;
import com.chagok.common.protocol.Request;
import com.chagok.common.protocol.Response;
import com.chagok.server.util.ServerLogger;
import com.google.gson.JsonSyntaxException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientHandler implements Runnable {
    static final int MAX_LINE = 1_048_576;
    private static final AtomicInteger SEQUENCE = new AtomicInteger();
    private static final int IDLE_TIMEOUT_MS = 30 * 60 * 1000;

    private final Socket socket;
    private final CommandDispatcher dispatcher;
    private final ClientSession session = new ClientSession();
    private final String threadName = "handler-" + SEQUENCE.incrementAndGet();

    public ClientHandler(Socket socket, CommandDispatcher dispatcher) {
        this.socket = socket;
        this.dispatcher = dispatcher;
    }

    @Override
    public void run() {
        Thread.currentThread().setName(threadName);
        try (Socket client = socket;
             BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8))) {
            client.setSoTimeout(IDLE_TIMEOUT_MS);
            String line;
            while ((line = in.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                long started = System.currentTimeMillis();
                Response response;
                String cmd = "-";
                if (line.length() > MAX_LINE) {
                    response = Response.errorRaw(JsonUtil.sniffReqId(line), null, ErrorCode.E100, "요청이 1MB를 초과했습니다");
                    write(out, response);
                    break;
                }
                try {
                    Request request = JsonUtil.parseRequest(line);
                    cmd = request.getCmdRaw();
                    response = dispatcher.dispatch(request, session);
                } catch (JsonSyntaxException ex) {
                    response = Response.errorRaw(JsonUtil.sniffReqId(line), null, ErrorCode.E100,
                            ErrorCode.E100.getDefaultMessage());
                }
                write(out, response);
                ServerLogger.info(threadName, cmd, session.getUserId(), response.getCode(),
                        System.currentTimeMillis() - started);
            }
        } catch (SocketTimeoutException ex) {
            ServerLogger.warn(threadName, "-", session.getUserId(), "idle timeout");
        } catch (IOException ex) {
            ServerLogger.warn(threadName, "-", session.getUserId(), "disconnected " + socket.getRemoteSocketAddress());
        }
    }

    private static void write(BufferedWriter out, Response response) throws IOException {
        out.write(JsonUtil.toJson(response));
        out.write('\n');
        out.flush();
    }
}
