package com.chagok.client.ui;

import com.chagok.client.AppContext;
import com.chagok.client.util.Alerts;
import com.chagok.client.util.Money;
import com.chagok.common.dto.DailySumDTO;
import com.chagok.common.dto.TransactionDTO;
import com.chagok.common.dto.WarningDTO;
import com.chagok.common.util.DateUtil;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainFrame extends JFrame {
    private static final String[] WEEK = {"일", "월", "화", "수", "목", "금", "토"};
    private static final java.lang.reflect.Type DAILY = new TypeToken<List<DailySumDTO>>() { }.getType();
    private static final Color TEAL = new Color(0x1F6F6A);
    private static final Color INCOME = new Color(0x1565C0);
    private static final Color EXPENSE = new Color(0xC62828);

    private final AppContext app;
    private YearMonth month;
    private LocalDate selected;
    private List<TransactionDTO> monthItems = List.of();
    private final Map<LocalDate, DailySumDTO> daily = new HashMap<>();
    private final JLabel monthLabel = new JLabel("", SwingConstants.CENTER);
    private final JLabel summary = new JLabel(" ");
    private final JLabel banner = new JLabel(" ", SwingConstants.LEFT);
    private final JButton prev = new JButton("◀");
    private final JButton next = new JButton("▶");
    private final DefaultTableModel calendarModel = new DefaultTableModel(6, 7) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable calendar = new JTable(calendarModel);
    private final DefaultTableModel listModel = new DefaultTableModel(
            new String[]{"", "구분", "카테고리", "유형", "메모", "금액"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable list = new JTable(listModel);
    private final List<TransactionDTO> visible = new ArrayList<>();

    public MainFrame(AppContext app) {
        super("차곡");
        this.app = app;
        this.month = initialMonth();
        this.selected = month.atDay(1);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1180, 760);
        setMinimumSize(new Dimension(1024, 680));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(0, 0));
        add(header(), BorderLayout.NORTH);
        add(center(), BorderLayout.CENTER);
        add(footer(), BorderLayout.SOUTH);
        styleCalendar();
        styleList();
        prev.addActionListener(e -> changeMonth(-1));
        next.addActionListener(e -> changeMonth(1));
        calendar.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = calendar.rowAtPoint(e.getPoint());
                int col = calendar.columnAtPoint(e.getPoint());
                LocalDate date = dateAt(row, col);
                if (date != null) {
                    selected = date;
                    calendar.repaint();
                    showDay();
                }
            }
        });
        list.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editSelected();
                }
            }
        });
        banner.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                openStats();
            }
        });
        reload();
    }

    private JPanel header() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 14, 8, 14));
        JLabel who = new JLabel(app.user.getUserName() + "  ·  " + app.user.getAge() + "세");
        who.setForeground(TEAL);
        panel.add(who, BorderLayout.WEST);
        JPanel nav = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        nav.setOpaque(false);
        monthLabel.setFont(monthLabel.getFont().deriveFont(Font.BOLD, 18f));
        nav.add(prev);
        nav.add(monthLabel);
        nav.add(next);
        panel.add(nav, BorderLayout.CENTER);
        panel.add(summary, BorderLayout.EAST);
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.add(panel, BorderLayout.NORTH);
        banner.setOpaque(true);
        banner.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        banner.setFont(banner.getFont().deriveFont(Font.BOLD, 14f));
        wrap.add(banner, BorderLayout.SOUTH);
        return wrap;
    }

    private JPanel center() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 8, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        panel.setBackground(new Color(0xF6F4EF));
        JScrollPane calendarScroll = new JScrollPane(calendar);
        calendarScroll.setBorder(BorderFactory.createTitledBorder("캘린더"));
        JScrollPane listScroll = new JScrollPane(list);
        listScroll.setBorder(BorderFactory.createTitledBorder("일별 내역"));
        panel.add(calendarScroll);
        panel.add(listScroll);
        return panel;
    }

    private JPanel footer() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton add = new JButton("＋ 내역 추가하기");
        JButton stats = new JButton("통계 보기");
        JButton edit = new JButton("선택 내역 수정");
        JButton delete = new JButton("선택 내역 삭제");
        JButton settings = new JButton("설정");
        add.addActionListener(e -> openEditor(null));
        stats.addActionListener(e -> openStats());
        edit.addActionListener(e -> editSelected());
        delete.addActionListener(e -> deleteSelected());
        settings.addActionListener(e -> new SettingsDialog(this, app, this::reload).setVisible(true));
        panel.add(add);
        panel.add(edit);
        panel.add(delete);
        panel.add(stats);
        panel.add(settings);
        return panel;
    }

    private void styleCalendar() {
        calendar.setRowHeight(78);
        calendar.setCellSelectionEnabled(true);
        calendar.setTableHeader(null);
        calendar.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                            boolean hasFocus, int row, int column) {
                JPanel cell = new JPanel(new BorderLayout());
                cell.setBorder(BorderFactory.createLineBorder(new Color(0xE4E0D8)));
                LocalDate date = dateAt(row, column);
                if (date == null) {
                    cell.setBackground(new Color(0xF7F7F7));
                    return cell;
                }
                boolean today = date.equals(DateUtil.today());
                boolean picked = date.equals(selected);
                cell.setBackground(today ? new Color(0xFFF4C2) : Color.WHITE);
                if (picked) {
                    cell.setBorder(BorderFactory.createLineBorder(TEAL, 2));
                }
                JLabel day = new JLabel(date.getDayOfMonth() + " " + WEEK[column]);
                day.setForeground(column == 0 ? EXPENSE : column == 6 ? INCOME : Color.DARK_GRAY);
                DailySumDTO sum = daily.get(date);
                JLabel income = new JLabel(sum == null || sum.getIncome() == 0 ? " " : "+" + Money.comma(sum.getIncome()));
                income.setForeground(INCOME);
                JLabel expense = new JLabel(sum == null || sum.getExpense() == 0 ? " " : "-" + Money.comma(sum.getExpense()));
                expense.setForeground(EXPENSE);
                JPanel text = new JPanel(new GridLayout(3, 1));
                text.setOpaque(false);
                text.add(day);
                text.add(income);
                text.add(expense);
                cell.add(text, BorderLayout.NORTH);
                return cell;
            }
        });
        calendar.setFillsViewportHeight(true);
    }

    private void styleList() {
        list.setRowHeight(28);
        list.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        list.getColumnModel().getColumn(5).setCellRenderer(right);
        list.getColumnModel().getColumn(0).setMaxWidth(36);
    }

    private void changeMonth(int delta) {
        month = month.plusMonths(delta);
        selected = month.atDay(1);
        reload();
    }

    private void reload() {
        monthLabel.setText(month.getYear() + "년 " + month.getMonthValue() + "월");
        prev.setEnabled(false);
        next.setEnabled(false);
        String ym = month.toString();
        new SwingWorker<Void, Void>() {
            private JsonObject monthBody;
            private WarningDTO warning;

            @Override
            protected Void doInBackground() {
                monthBody = app.api.listMonth(ym);
                warning = app.api.warning(ym);
                return null;
            }

            @Override
            protected void done() {
                prev.setEnabled(true);
                next.setEnabled(true);
                try {
                    get();
                    applyMonth(monthBody);
                    applyWarning(warning);
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    if (cause instanceof Exception exception) {
                        Alerts.error(MainFrame.this, exception);
                    }
                }
            }
        }.execute();
    }

    private void applyMonth(JsonObject body) {
        monthItems = app.api.transactions(body.get("items"));
        List<DailySumDTO> sums = com.chagok.common.protocol.JsonUtil.fromJson(body.get("daily"), DAILY);
        daily.clear();
        if (sums != null) {
            for (DailySumDTO sum : sums) {
                daily.put(LocalDate.parse(sum.getDate()), sum);
            }
        }
        if (selected == null || !YearMonth.from(selected).equals(month)) {
            selected = month.atDay(1);
        }
        long income = body.get("monthIncome").getAsLong();
        long expense = body.get("monthExpense").getAsLong();
        summary.setText("수입 " + Money.won(income) + "   지출 " + Money.won(expense)
                + "   잔액 " + Money.won(income - expense) + "  ");
        calendarModel.setRowCount(6);
        calendar.repaint();
        showDay();
    }

    private void applyWarning(WarningDTO warning) {
        banner.setText(warning.getMessage() == null ? " " : "  " + warning.getMessage());
        Color background = switch (warning.getLevel() == null ? "" : warning.getLevel()) {
            case "SAFE" -> new Color(0xE5F6E8);
            case "CAUTION" -> new Color(0xFFF6D8);
            case "DANGER" -> new Color(0xFDE2E1);
            default -> new Color(0xFFE4CC);
        };
        banner.setBackground(background);
        if (warning.isShowPopup()) {
            JOptionPane.showMessageDialog(this, warning.getMessage(), "소비 습관 알림", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void showDay() {
        listModel.setRowCount(0);
        visible.clear();
        for (TransactionDTO item : monthItems) {
            if (selected.equals(item.getTransactionDate())) {
                visible.add(item);
                String kind = "INCOME".equals(item.getType()) ? "수입" : "지출";
                String fixed = "FIXED".equals(item.getFixedType()) ? "고정" : "VARIABLE".equals(item.getFixedType()) ? "유동" : "";
                listModel.addRow(new Object[]{
                        item.isAuto() ? "↻" : "",
                        kind,
                        item.getCategoryName(),
                        fixed,
                        item.getMemo() == null ? "" : item.getMemo(),
                        ("INCOME".equals(item.getType()) ? "+" : "-") + Money.comma(item.getAmount())
                });
            }
        }
        if (visible.isEmpty()) {
            listModel.addRow(new Object[]{"", "", "등록된 내역이 없음", "", "", ""});
        }
    }

    private void openEditor(TransactionDTO editing) {
        TransactionDialog dialog = new TransactionDialog(this, app, editing, selected);
        if (dialog.showDialog()) {
            reload();
        }
    }

    private void editSelected() {
        TransactionDTO item = selectedItem();
        if (item != null) {
            openEditor(item);
        }
    }

    private void deleteSelected() {
        TransactionDTO item = selectedItem();
        if (item == null) {
            return;
        }
        JCheckBox together = new JCheckBox("자동 생성 내역도 함께 삭제");
        Object message = "선택한 내역을 삭제할까요?";
        boolean template = item.getParentTxId() == null && item.getRepeatCycle() != null && !"NONE".equals(item.getRepeatCycle());
        if (template) {
            JPanel panel = new JPanel(new BorderLayout());
            panel.add(new JLabel("선택한 내역을 삭제할까요?"), BorderLayout.NORTH);
            panel.add(together, BorderLayout.SOUTH);
            message = panel;
        }
        int answer = JOptionPane.showConfirmDialog(this, message, "삭제", JOptionPane.OK_CANCEL_OPTION);
        if (answer != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            app.api.deleteTransaction(item.getTransactionId(), template && together.isSelected());
            reload();
        } catch (RuntimeException ex) {
            Alerts.error(this, ex);
        }
    }

    private TransactionDTO selectedItem() {
        int row = list.getSelectedRow();
        if (row < 0 || row >= visible.size()) {
            Alerts.info(this, "내역을 선택해 주세요");
            return null;
        }
        return visible.get(row);
    }

    private void openStats() {
        new StatisticsFrame(app, month).setVisible(true);
    }

    private LocalDate dateAt(int row, int column) {
        if (row < 0 || column < 0) {
            return null;
        }
        int lead = month.atDay(1).getDayOfWeek().getValue() % 7;
        int day = row * 7 + column - lead + 1;
        if (day < 1 || day > month.lengthOfMonth()) {
            return null;
        }
        return month.atDay(day);
    }

    private static YearMonth initialMonth() {
        YearMonth now = YearMonth.now(DateUtil.KST);
        if (now.equals(YearMonth.of(2026, 11)) || now.equals(YearMonth.of(2026, 12))) {
            return YearMonth.of(2026, 10);
        }
        return now;
    }
}
