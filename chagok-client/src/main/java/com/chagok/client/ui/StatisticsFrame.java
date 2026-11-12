package com.chagok.client.ui;

import com.chagok.client.AppContext;
import com.chagok.client.chart.ChartFactory;
import com.chagok.client.net.ApiException;
import com.chagok.client.util.Alerts;
import com.chagok.client.util.Money;
import com.chagok.common.dto.CategoryStatDTO;
import com.chagok.common.dto.CompareDTO;
import com.chagok.common.dto.MonthlyStatDTO;
import com.chagok.common.protocol.ErrorCode;
import com.google.gson.JsonObject;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.time.YearMonth;
import java.util.List;

public class StatisticsFrame extends JFrame {
    private final AppContext app;
    private YearMonth month;
    private final JLabel title = new JLabel();
    private final JLabel diff = new JLabel(" ");
    private final JCheckBox sameGender = new JCheckBox("같은 성별만");
    private final JPanel pieHolder = holder();
    private final JPanel barHolder = holder();
    private final JPanel peerHolder = holder();
    private final JButton prev = new JButton("◀");
    private final JButton next = new JButton("▶");

    public StatisticsFrame(AppContext app, YearMonth month) {
        super("차곡 통계");
        this.app = app;
        this.month = month;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(980, 680);
        setLocationRelativeTo(null);
        JPanel top = new JPanel(new BorderLayout());
        JPanel nav = new JPanel(new FlowLayout(FlowLayout.LEFT));
        prev.addActionListener(e -> move(-1));
        next.addActionListener(e -> move(1));
        nav.add(prev);
        nav.add(title);
        nav.add(next);
        top.add(nav, BorderLayout.WEST);
        top.add(sameGender, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("카테고리", pieHolder);
        JPanel monthTab = new JPanel(new BorderLayout());
        monthTab.add(barHolder, BorderLayout.CENTER);
        diff.setBorder(BorderFactory.createEmptyBorder(8, 12, 12, 12));
        monthTab.add(diff, BorderLayout.SOUTH);
        tabs.addTab("전월 비교", monthTab);
        tabs.addTab("또래 비교", peerHolder);
        add(tabs, BorderLayout.CENTER);
        sameGender.addActionListener(e -> load());
        load();
    }

    private void move(int delta) {
        month = month.plusMonths(delta);
        load();
    }

    private void load() {
        title.setText("  " + month + "  ");
        prev.setEnabled(false);
        next.setEnabled(false);
        String ym = month.toString();
        boolean gender = sameGender.isSelected();
        new SwingWorker<Void, Void>() {
            private List<CategoryStatDTO> pie;
            private long total;
            private MonthlyStatDTO current;
            private MonthlyStatDTO previous;
            private Long diffAmount;
            private Double diffRate;
            private CompareDTO compare;
            private String peerError;

            @Override
            protected Void doInBackground() {
                JsonObject category = app.api.categoryStat(ym);
                pie = app.api.statItems(category.get("items"));
                total = category.get("total").getAsLong();
                JsonObject monthly = app.api.monthly(ym);
                current = app.api.monthlyOf(monthly.get("current"));
                previous = app.api.monthlyOf(monthly.get("previous"));
                diffAmount = monthly.get("diffAmount").getAsLong();
                diffRate = monthly.get("diffRate").isJsonNull() ? null : monthly.get("diffRate").getAsDouble();
                try {
                    compare = app.api.compare(ym, gender);
                } catch (ApiException ex) {
                    if (ex.getCode() == ErrorCode.E401) {
                        peerError = ex.getMessage();
                    } else {
                        throw ex;
                    }
                }
                return null;
            }

            @Override
            protected void done() {
                prev.setEnabled(true);
                next.setEnabled(true);
                try {
                    get();
                    showPie();
                    showMonth();
                    showPeer();
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    if (cause instanceof Exception exception) {
                        Alerts.error(StatisticsFrame.this, exception);
                    }
                }
            }

            private void showPie() {
                pieHolder.removeAll();
                if (pie == null || pie.isEmpty() || total == 0) {
                    pieHolder.add(message("이번 달 지출 내역이 없음"));
                } else {
                    pieHolder.add(ChartFactory.pie(pie), BorderLayout.CENTER);
                }
                pieHolder.revalidate();
                pieHolder.repaint();
            }

            private void showMonth() {
                barHolder.removeAll();
                barHolder.add(ChartFactory.monthBars(previous, current), BorderLayout.CENTER);
                barHolder.revalidate();
                barHolder.repaint();
                if (diffRate == null) {
                    diff.setText("전월 데이터가 없어 증감률은 - 입니다. 당월 지출 " + Money.won(current.getExpense()));
                    diff.setForeground(new Color(0x555555));
                } else {
                    boolean up = diffAmount >= 0;
                    diff.setForeground(up ? new Color(0xC62828) : new Color(0x1565C0));
                    diff.setText(String.format("전월 대비 지출 %s%s원 (%s%.1f%%) %s",
                            up ? "+" : "", Money.comma(diffAmount), up ? "+" : "", diffRate, up ? "▲" : "▼"));
                }
            }

            private void showPeer() {
                peerHolder.removeAll();
                if (peerError != null) {
                    peerHolder.add(message(peerError));
                } else if (compare == null || compare.getItems().isEmpty()) {
                    peerHolder.add(message("비교할 지출이 없음"));
                } else {
                    JPanel wrap = new JPanel(new BorderLayout());
                    wrap.setBackground(Color.WHITE);
                    wrap.add(new JLabel(String.format("  %d대 · 표본 %d명 · 내 유동비율 %s / 또래 %s",
                            compare.getAgeGroup(), compare.getSampleSize(),
                            compare.getMyRatio() == null ? "-" : compare.getMyRatio() + "%",
                            compare.getPeerRatio() == null ? "-" : compare.getPeerRatio() + "%")), BorderLayout.NORTH);
                    wrap.add(ChartFactory.peerBars(compare.getItems()), BorderLayout.CENTER);
                    peerHolder.add(wrap, BorderLayout.CENTER);
                }
                peerHolder.revalidate();
                peerHolder.repaint();
            }
        }.execute();
    }

    private static JPanel holder() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        return panel;
    }

    private static JLabel message(String text) {
        JLabel label = new JLabel(text, JLabel.CENTER);
        label.setForeground(new Color(0x666666));
        return label;
    }
}
