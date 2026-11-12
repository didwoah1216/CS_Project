package com.chagok.client.net;

import com.chagok.common.protocol.ErrorCode;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

public class ServerConnection {
    private final String host;
    private final int port;
    private final int timeoutMs;
    private final Object lock = new Object();
    private Socket socket;
    private BufferedReader in;
    private BufferedWriter out;

    public ServerConnection(ClientConfig config) {
        this.host = config.host();
        this.port = config.port();
        this.timeoutMs = config.timeoutMs();
    }

    public void connect() {
        synchronized (lock) {
            IOException last = null;
            for (int attempt = 1; attempt <= 3; attempt++) {
                try {
                    open();
                    return;
                } catch (IOException ex) {
                    last = ex;
                    close();
                    if (attempt < 3) {
                        sleep();
                    }
                }
            }
            throw new ApiException(ErrorCode.C901, ErrorCode.C901.getDefaultMessage()
                    + (last == null ? "" : " (" + last.getMessage() + ")"));
        }
    }

    public String exchange(String json) {
        synchronized (lock) {
            try {
                if (!isConnected()) {
                    throw new IOException("연결 없음");
                }
                out.write(json);
                out.write('\n');
                out.flush();
                socket.setSoTimeout(timeoutMs);
                String line = in.readLine();
                if (line == null) {
                    throw new IOException("서버가 연결을 닫았습니다");
                }
                return line;
            } catch (SocketTimeoutException ex) {
                close();
                throw new ApiException(ErrorCode.C902, ErrorCode.C902.getDefaultMessage());
            } catch (IOException ex) {
                close();
                throw new ApiException(ErrorCode.C901, ErrorCode.C901.getDefaultMessage());
            }
        }
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    public void close() {
        synchronized (lock) {
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException ignored) {
                // 이미 닫힌 소켓
            }
            socket = null;
            in = null;
            out = null;
        }
    }

    private void open() throws IOException {
        closeUnlocked();
        Socket next = new Socket();
        next.connect(new InetSocketAddress(host, port), timeoutMs);
        next.setTcpNoDelay(true);
        socket = next;
        in = new BufferedReader(new InputStreamReader(next.getInputStream(), StandardCharsets.UTF_8));
        out = new BufferedWriter(new OutputStreamWriter(next.getOutputStream(), StandardCharsets.UTF_8));
    }

    private void closeUnlocked() {
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignored) {
            // 재연결 전 정리
        }
        socket = null;
        in = null;
        out = null;
    }

    private static void sleep() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
