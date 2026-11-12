package com.chagok.server;

import com.chagok.server.dao.CategoryDAO;
import com.chagok.server.dao.TransactionDAO;
import com.chagok.server.dao.UserDAO;
import com.chagok.server.dao.UserSettingDAO;
import com.chagok.server.service.CompareService;
import com.chagok.server.service.FeedbackService;
import com.chagok.server.service.SettingService;
import com.chagok.server.service.StatisticsService;
import com.chagok.server.service.TestAccountService;
import com.chagok.server.service.TransactionService;
import com.chagok.server.util.DummyDataLoader;
import com.chagok.server.util.ServerConfig;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ChagokServer {
    private final ServerConfig config;
    private final CommandDispatcher dispatcher;
    private ServerSocket serverSocket;
    private ExecutorService pool;
    private volatile boolean running;

    public ChagokServer(ServerConfig config) {
        this.config = config;
        UserDAO users = new UserDAO();
        CategoryDAO categories = new CategoryDAO();
        TransactionDAO transactions = new TransactionDAO();
        UserSettingDAO settings = new UserSettingDAO();
        CompareService compare = new CompareService(users, categories, transactions, settings);
        this.dispatcher = new CommandDispatcher(
                new TestAccountService(users, new DummyDataLoader()),
                new TransactionService(transactions, categories),
                new StatisticsService(transactions, categories),
                new FeedbackService(transactions, settings, compare),
                compare,
                new SettingService(settings),
                categories);
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(config.port());
        AtomicInteger ids = new AtomicInteger();
        pool = Executors.newFixedThreadPool(config.maxClients(), runnable -> {
            Thread thread = new Thread(runnable, "pool-" + ids.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
        running = true;
        Thread accept = new Thread(this::acceptLoop, "accept");
        accept.setDaemon(true);
        accept.start();
        System.out.println("Listening on " + config.port());
    }

    private void acceptLoop() {
        while (running) {
            try {
                var socket = serverSocket.accept();
                pool.submit(new ClientHandler(socket, dispatcher));
            } catch (IOException ex) {
                if (running) {
                    System.out.println("accept 중단: " + ex.getMessage());
                }
                break;
            }
        }
    }

    public void shutdown() {
        running = false;
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
                // 종료 중
            }
        }
        if (pool != null) {
            pool.shutdownNow();
            try {
                pool.awaitTermination(2, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public int port() {
        return serverSocket == null ? config.port() : serverSocket.getLocalPort();
    }
}
