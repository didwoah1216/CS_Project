package com.chagok.client.ui;

import com.chagok.client.AppContext;
import com.chagok.client.util.Alerts;
import com.chagok.client.util.DigitsOnlyFilter;
import com.chagok.client.util.Money;
import com.chagok.common.dto.BudgetDTO;
import com.chagok.common.dto.CategoryDTO;
import com.chagok.common.util.BudgetPolicy;
import com.google.gson.JsonObject;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.AbstractDocument;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** SCR-05 예산 관리. */
public class BudgetFrame extends JFrame {
    private final AppContext app;
    private YearMonth month;
    private final JLabel title = new JLabel("", SwingConstants.CENTER);
    private final JLabel hint = new JLabel("");
    private final JButton prev = new JButton("◀");
    private final JButton next = new JButton("▶");
    private final JComboBox<CategoryDTO> categories = new JComboBox<>();
    private final JTextField amount = new JTextField(10);
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"카테고리", "예산", "사용액", "사용률", "상태"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private final List<BudgetDTO> rows = new ArrayList<>();
    private int alertRatio = 80;

    public BudgetFrame(AppContext app, YearMonth month) {
        super("차곡 예산");
        this.app = app;
        this.month = month;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(760, 520);
        setLocationRelativeTo(null);
        add(header(), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(editor(), BorderLayout.SOUTH);
        styleTable();
        fillCategories();
        prev.addActionListener(e -> move(-1));
        next.addActionListener(e -> move(1));
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                fillFromSelection();
            }
        });
        reload();
    }

    private JPanel header() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        panel.add(prev);
        panel.add(title);
        panel.add(next);
        panel.add(hint);
        return panel;
    }

    private JPanel editor() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        panel.setBorder(BorderFactory.createTitledBorder("예산 설정"));
        ((AbstractDocument) amount.getDocument()).setDocumentFilter(new DigitsOnlyFilter(9));
        categories.setRenderer((list, value, index, selected, focus) -> {
            JLabel label = new JLabel(value == null ? " " : value.getName());
            label.setOpaque(true);
            if (selected) {
                label.setBackground(list.getSelectionBackground());
                label.setForeground(list.getSelectionForeground());
            }
            return label;
        });
        JButton save = new JButton("저장");
        JButton delete = new JButton("삭제");
        JButton copy = new JButton("전월 복사");
        save.addActionListener(e -> save());
        delete.addActionListener(e -> delete());
        copy.addActionListener(e -> copy());
        panel.add(new JLabel("카테고리"));
        panel.add(categories);
        panel.add(new JLabel("금액"));
        panel.add(amount);
        panel.add(save);
        panel.add(delete);
        panel.add(copy);
        return panel;
    }

    private void styleTable() {
        table.setRowHeight(28);
        table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        table.getColumnModel().getColumn(1).setCellRenderer(right);
        table.getColumnModel().getColumn(2).setCellRenderer(right);
        table.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            private final JProgressBar bar = new JProgressBar(0, 100);

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                            boolean hasFocus, int row, int column) {
                bar.setStringPainted(true);
                bar.setString(value == null ? "" : value.toString());
                double rate = rows.size() > row ? rows.get(row).getUsageRate() : 0;
                bar.setValue((int) Math.min(100, Math.round(rate)));
                String status = rows.size() > row ? rows.get(row).getStatus() : "";
                Color color = switch (status == null ? "" : status) {
                    case BudgetPolicy.OVER -> new Color(0xE57373);
                    case BudgetPolicy.ALERT -> new Color(0xF6C945);
                    default -> new Color(0x7DCEA0);
                };
                bar.setForeground(color);
                bar.setBackground(Color.WHITE);
                return bar;
            }
        });
    }

    private void fillCategories() {
        categories.removeAllItems();
        for (CategoryDTO category : app.categories) {
            if ("EXPENSE".equals(category.getType())) {
                categories.addItem(category);
            }
        }
    }

    private void move(int delta) {
        month = month.plusMonths(delta);
        reload();
    }

    private void reload() {
        title.setText(month.getYear() + "년 " + month.getMonthValue() + "월");
        prev.setEnabled(false);
        next.setEnabled(false);
        String ym = month.toString();
        new SwingWorker<JsonObject, Void>() {
            @Override
            protected JsonObject doInBackground() {
                return app.api.budgetList(ym);
            }

            @Override
            protected void done() {
                prev.setEnabled(true);
                next.setEnabled(true);
                try {
                    apply(get());
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    if (cause instanceof Exception exception) {
                        Alerts.error(BudgetFrame.this, exception);
                    }
                }
            }
        }.execute();
    }

    private void apply(JsonObject body) {
        alertRatio = body.get("alertRatio").getAsInt();
        hint.setText("  알림 기준 " + alertRatio + "%");
        rows.clear();
        rows.addAll(app.api.budgets(body.get("items")));
        model.setRowCount(0);
        for (BudgetDTO item : rows) {
            model.addRow(new Object[]{
                    item.getCategoryName(),
                    Money.won(item.getAmount()),
                    Money.won(item.getUsed()),
                    String.format("%.1f%%", item.getUsageRate()),
                    label(item.getStatus())
            });
        }
    }

    private void fillFromSelection() {
        int index = table.getSelectedRow();
        if (index < 0 || index >= rows.size()) {
            return;
        }
        BudgetDTO item = rows.get(index);
        amount.setText(Integer.toString(item.getAmount()));
        for (int i = 0; i < categories.getItemCount(); i++) {
            if (categories.getItemAt(i).getCategoryId() == item.getCategoryId()) {
                categories.setSelectedIndex(i);
                break;
            }
        }
    }

    private void save() {
        CategoryDTO category = (CategoryDTO) categories.getSelectedItem();
        if (category == null) {
            Alerts.info(this, "카테고리를 선택해 주세요");
            return;
        }
        if (amount.getText().isBlank()) {
            Alerts.info(this, "금액을 입력해 주세요");
            return;
        }
        int value = Integer.parseInt(amount.getText());
        try {
            app.api.budgetSet(category.getCategoryId(), month.toString(), value);
            reload();
        } catch (RuntimeException ex) {
            Alerts.error(this, ex);
        }
    }

    private void delete() {
        int index = table.getSelectedRow();
        if (index < 0 || index >= rows.size()) {
            Alerts.info(this, "삭제할 예산을 선택해 주세요");
            return;
        }
        BudgetDTO item = rows.get(index);
        int answer = JOptionPane.showConfirmDialog(this,
                item.getCategoryName() + " 예산을 삭제할까요?", "삭제", JOptionPane.OK_CANCEL_OPTION);
        if (answer != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            app.api.budgetDelete(item.getBudgetId());
            reload();
        } catch (RuntimeException ex) {
            Alerts.error(this, ex);
        }
    }

    private void copy() {
        int answer = JOptionPane.showConfirmDialog(this,
                "전월 예산을 이번 달로 복사할까요? 같은 카테고리는 전월 금액으로 바뀝니다.",
                "전월 복사", JOptionPane.OK_CANCEL_OPTION);
        if (answer != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            JsonObject body = app.api.budgetCopy(month.toString());
            apply(body);
            Alerts.info(this, body.get("copied").getAsInt() + "건을 복사했습니다");
        } catch (RuntimeException ex) {
            Alerts.error(this, ex);
        }
    }

    private static String label(String status) {
        return switch (status == null ? "" : status) {
            case BudgetPolicy.OVER -> "초과";
            case BudgetPolicy.ALERT -> "주의";
            case BudgetPolicy.NORMAL -> "정상";
            default -> status;
        };
    }
}
