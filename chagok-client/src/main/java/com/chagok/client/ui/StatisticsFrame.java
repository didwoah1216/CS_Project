package com.chagok.client.ui;

import com.chagok.client.AppContext;
import com.chagok.client.chart.ChartFactory;
import com.chagok.client.net.ApiException;
import com.chagok.client.util.Alerts;
import com.chagok.client.util.Money;
import com.chagok.common.dto.CategoryStatDTO;
import com.chagok.common.dto.CompareDTO;
import com.chagok.common.dto.MonthlyStatDTO;
import com.chagok.common.dto.ScoreDTO;
import com.chagok.common.dto.WeekdayStatDTO;
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
    private final JPanel trendHolder = holder();
    private final JPanel scoreHolder = holder();
    private final JPanel weekdayHolder = holder();
    private final JLabel weekdayNote = new JLabel(" ");
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
        tabs.addTab("6개월 추이", trendHolder);
        tabs.addTab("소비 점수", scoreHolder);
        JPanel weekdayTab = new JPanel(new BorderLayout());
        weekdayTab.add(weekdayHolder, BorderLayout.CENTER);
        weekdayNote.setBorder(BorderFactory.createEmptyBorder(8, 12, 12, 12));
        weekdayTab.add(weekdayNote, BorderLayout.SOUTH);
        tabs.addTab("요일 패턴", weekdayTab);
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
            private List<MonthlyStatDTO> trend;
            private ScoreDTO score;
            private List<WeekdayStatDTO> weekdays;
            private String weekdayMessage;

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
                trend = app.api.months(app.api.trend(ym).get("items"));
                score = app.api.score(ym, true);
                JsonObject weekday = app.api.weekday(ym);
                weekdays = app.api.weekdays(weekday.get("items"));
                weekdayMessage = weekday.get("message").getAsString();
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
                    showTrend();
                    showScore();
                    showWeekday();
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

            private void showTrend() {
                trendHolder.removeAll();
                boolean empty = trend == null || trend.stream().allMatch(item -> item.getIncome() == 0 && item.getExpense() == 0);
                if (empty) {
                    trendHolder.add(message("표시할 6개월 추이가 없습니다"));
                } else {
                    trendHolder.add(ChartFactory.trend(trend), BorderLayout.CENTER);
                }
                trendHolder.revalidate();
                trendHolder.repaint();
            }

            private void showScore() {
                scoreHolder.removeAll();
                if (score == null) {
                    scoreHolder.add(message("점수를 계산하지 못했습니다"));
                } else {
                    StringBuilder html = new StringBuilder("<html><div style='padding:12px;font-family:sans-serif'>");
                    html.append("<div style='font-size:22px'><b>").append(score.getScore()).append("점 ")
                            .append(score.getGrade()).append("</b></div>");
                    if (score.getFlexRatio() != null) {
                        html.append("<div>유동비율 ").append(String.format("%.1f", score.getFlexRatio())).append("%</div>");
                    }
                    if (score.getItems() != null) {
                        for (ScoreDTO.ScoreItem item : score.getItems()) {
                            html.append("<div>").append(item.getName()).append(" ")
                                    .append(item.getPoint()).append(" / ").append(item.getMax()).append("</div>");
                        }
                    }
                    html.append("<div style='margin-top:8px'>").append(score.getMessage() == null ? "" : score.getMessage())
                            .append("</div>");
                    if (score.getPrevScore() != null) {
                        html.append("<div>저장된 전월 점수 ").append(score.getPrevScore()).append("점</div>");
                    }
                    html.append("</div></html>");
                    JLabel label = new JLabel(html.toString());
                    label.setVerticalAlignment(JLabel.TOP);
                    scoreHolder.add(label, BorderLayout.NORTH);
                }
                scoreHolder.revalidate();
                scoreHolder.repaint();
            }

            private void showWeekday() {
                weekdayHolder.removeAll();
                boolean empty = weekdays == null || weekdays.stream().allMatch(item -> item.getAmount() == 0);
                if (empty) {
                    weekdayHolder.add(message("이 달의 유동지출이 없습니다"));
                    weekdayNote.setText(" ");
                } else {
                    weekdayHolder.add(ChartFactory.weekday(weekdays), BorderLayout.CENTER);
                    weekdayNote.setText(weekdayMessage == null ? " " : weekdayMessage);
                }
                weekdayHolder.revalidate();
                weekdayHolder.repaint();
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
