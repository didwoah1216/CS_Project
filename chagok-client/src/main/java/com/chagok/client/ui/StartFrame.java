package com.chagok.client.ui;

import com.chagok.client.AppContext;
import com.chagok.client.net.ApiException;
import com.chagok.client.util.Alerts;
import com.chagok.common.protocol.ErrorCode;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

public class StartFrame extends JFrame {
    private final AppContext app;
    private final JLabel status = new JLabel("연결 중", SwingConstants.CENTER);
    private final JButton start = new JButton("시작하기");

    public StartFrame(AppContext app) {
        super("차곡");
        this.app = app;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(460, 520);
        setLocationRelativeTo(null);
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(BorderFactory.createEmptyBorder(36, 32, 24, 32));
        root.setBackground(new Color(0xF6F4EF));
        JLabel title = new JLabel("차곡", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 48f));
        title.setForeground(new Color(0x1F6F6A));
        JLabel sub = new JLabel("<html><center>20대 소비습관 개선 가계부<br>테스트 계정 test01 로 시작합니다</center></html>",
                SwingConstants.CENTER);
        JPanel head = new JPanel(new BorderLayout(0, 8));
        head.setOpaque(false);
        head.add(title, BorderLayout.NORTH);
        head.add(sub, BorderLayout.CENTER);
        root.add(head, BorderLayout.NORTH);
        start.setEnabled(false);
        start.setFont(start.getFont().deriveFont(Font.BOLD, 16f));
        start.setPreferredSize(new Dimension(200, 44));
        start.addActionListener(e -> {
            if (app.connection.isConnected()) {
                begin();
            } else {
                connect();
            }
        });
        JPanel center = new JPanel();
        center.setOpaque(false);
        center.add(start);
        root.add(center, BorderLayout.CENTER);
        status.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        root.add(status, BorderLayout.SOUTH);
        setContentPane(root);
        connect();
    }

    private void connect() {
        status.setText("● 연결 중");
        status.setForeground(new Color(0x8A6A00));
        start.setEnabled(false);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                app.connection.connect();
                app.api.ping();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    status.setText("● 연결됨  " + app.config.host() + ":" + app.config.port());
                    status.setForeground(new Color(0x1B7A32));
                    start.setText("시작하기");
                    start.setEnabled(true);
                } catch (Exception ex) {
                    status.setText("● 연결 실패 · 다시 연결");
                    status.setForeground(new Color(0xC62828));
                    start.setText("다시 연결");
                    start.setEnabled(true);
                }
            }
        }.execute();
    }

    private void begin() {
        if (!app.connection.isConnected()) {
            connect();
            return;
        }
        start.setEnabled(false);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                app.user = app.api.testStart();
                app.categories = app.api.categories(null);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    MainFrame main = new MainFrame(app);
                    main.setVisible(true);
                    dispose();
                } catch (Exception ex) {
                    start.setEnabled(true);
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    if (cause instanceof ApiException api && api.getCode() == ErrorCode.E204) {
                        start.setEnabled(false);
                        status.setText("테스트 계정 모드가 꺼져 있음");
                    }
                    if (cause instanceof Exception exception) {
                        Alerts.error(StartFrame.this, exception);
                    }
                }
            }
        }.execute();
    }
}
