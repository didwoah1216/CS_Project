package com.chagok.server;

import com.chagok.server.util.ServerConfig;

public final class ServerMain {
    private ServerMain() {
    }

    public static void main(String[] args) throws Exception {
        ServerConfig config = ServerConfig.load();
        ChagokServer server = new ChagokServer(config);
        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown, "shutdown"));
        Thread.currentThread().join();
    }
}
