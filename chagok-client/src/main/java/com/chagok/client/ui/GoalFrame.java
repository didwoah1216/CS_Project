package com.chagok.client.ui;

import com.chagok.client.AppContext;
import com.chagok.client.util.Alerts;
import com.chagok.client.util.DigitsOnlyFilter;
import com.chagok.client.util.Money;
import com.chagok.common.dto.GoalDTO;
import com.chagok.common.util.DateUtil;
import com.google.gson.JsonObject;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.AbstractDocument;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** SCR-06 저축 목표. */
public class GoalFrame extends JFrame {
    private final AppContext app;
    private final JTextField name = new JTextField(12);
    private final JTextField target = new JTextField(8);
    private final JTextField start = new JTextField(10);
    private final JTextField end = new JTextField(10);
    private final JTextField depositAmount = new JTextField(8);
    private final JTextField depositDate = new JTextField(10);
    private final JTextField depositMemo = new JTextField(10);
    private final JCheckBox withdraw = new JCheckBox("인출");
    private final DefaultTableModel goalsModel = new DefaultTableModel(
            new String[]{"목표", "목표액", "모은 금액", "진행률", "월 필요액", "D-day", "상태"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final DefaultTableModel depositModel = new DefaultTableModel(
            new String[]{"날짜", "금액", "메모"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable goalsTable = new JTable(goalsModel);
    private final List<GoalDTO> goals = new ArrayList<>();

    public GoalFrame(AppContext app) {
        super("차곡 저축 목표");
        this.app = app;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(980, 620);
        setLocationRelativeTo(null);
        LocalDate today = DateUtil.today();
        start.setText(today.toString());
        end.setText(today.plusMonths(6).toString());
        depositDate.setText(today.toString());
        ((AbstractDocument) target.getDocument()).setDocumentFilter(new DigitsOnlyFilter(10));
        ((AbstractDocument) depositAmount.getDocument()).setDocumentFilter(new DigitsOnlyFilter(9));
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(goalsTable), new JScrollPane(new JTable(depositModel)));
        split.setResizeWeight(0.62);
        add(split, BorderLayout.CENTER);
        add(forms(), BorderLayout.SOUTH);
        goalsTable.setRowHeight(26);
        goalsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showDeposits();
            }
        });
        reload();
    }

    private JPanel forms() {
        JPanel panel = new JPanel(new GridLayout(2, 1));
        JPanel add = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        add.setBorder(BorderFactory.createTitledBorder("목표 추가"));
        JButton save = new JButton("추가");
        JButton remove = new JButton("삭제");
        save.addActionListener(e -> addGoal());
        remove.addActionListener(e -> deleteGoal());
        add.add(new JLabel("이름"));
        add.add(name);
        add.add(new JLabel("목표액"));
        add.add(target);
        add.add(new JLabel("시작"));
        add.add(start);
        add.add(new JLabel("목표일"));
        add.add(end);
        add.add(save);
        add.add(remove);
        JPanel pay = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        pay.setBorder(BorderFactory.createTitledBorder("입금 / 인출"));
        JButton apply = new JButton("반영");
        apply.addActionListener(e -> deposit());
        pay.add(new JLabel("금액"));
        pay.add(depositAmount);
        pay.add(withdraw);
        pay.add(new JLabel("날짜"));
        pay.add(depositDate);
        pay.add(new JLabel("메모"));
        pay.add(depositMemo);
        pay.add(apply);
        panel.add(add);
        panel.add(pay);
        return panel;
    }

    private void reload() {
        new SwingWorker<JsonObject, Void>() {
            @Override
            protected JsonObject doInBackground() {
                return app.api.goalList();
            }

            @Override
            protected void done() {
                try {
                    apply(get());
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    if (cause instanceof Exception exception) {
                        Alerts.error(GoalFrame.this, exception);
                    }
                }
            }
        }.execute();
    }

    private void apply(JsonObject body) {
        int selected = goalsTable.getSelectedRow();
        goals.clear();
        goals.addAll(app.api.goals(body.get("items")));
        goalsModel.setRowCount(0);
        for (GoalDTO goal : goals) {
            String day = goal.getDDay() >= 0 ? "D-" + goal.getDDay() : "D+" + Math.abs(goal.getDDay());
            goalsModel.addRow(new Object[]{
                    goal.getGoalName(),
                    Money.won(goal.getTargetAmount()),
                    Money.won(goal.getSavedAmount()),
                    String.format("%.1f%%", goal.getProgress()),
                    Money.won(goal.getMonthlyNeed()),
                    day,
                    status(goal.getStatus())
            });
        }
        if (selected >= 0 && selected < goals.size()) {
            goalsTable.setRowSelectionInterval(selected, selected);
        }
        showDeposits();
    }

    private void showDeposits() {
        depositModel.setRowCount(0);
        int index = goalsTable.getSelectedRow();
        if (index < 0 || index >= goals.size()) {
            return;
        }
        for (GoalDTO.DepositDTO deposit : goals.get(index).getDeposits()) {
            depositModel.addRow(new Object[]{
                    deposit.getDepositDate(),
                    (deposit.getAmount() > 0 ? "+" : "") + Money.comma(deposit.getAmount()),
                    deposit.getMemo() == null ? "" : deposit.getMemo()
            });
        }
    }

    private void addGoal() {
        if (name.getText().isBlank() || target.getText().isBlank()) {
            Alerts.info(this, "이름과 목표 금액을 입력해 주세요");
            return;
        }
        try {
            app.api.goalAdd(name.getText().trim(), Integer.parseInt(target.getText()), start.getText().trim(), end.getText().trim());
            name.setText("");
            target.setText("");
            reload();
        } catch (RuntimeException ex) {
            Alerts.error(this, ex);
        }
    }

    private void deposit() {
        GoalDTO goal = selected();
        if (goal == null || depositAmount.getText().isBlank()) {
            Alerts.info(this, "목표와 금액을 입력해 주세요");
            return;
        }
        int amount = Integer.parseInt(depositAmount.getText());
        if (withdraw.isSelected()) {
            amount = -amount;
        }
        try {
            app.api.goalDeposit(goal.getGoalId(), amount, depositDate.getText().trim(), depositMemo.getText());
            depositAmount.setText("");
            depositMemo.setText("");
            reload();
        } catch (RuntimeException ex) {
            Alerts.error(this, ex);
        }
    }

    private void deleteGoal() {
        GoalDTO goal = selected();
        if (goal == null) {
            Alerts.info(this, "삭제할 목표를 선택해 주세요");
            return;
        }
        int answer = JOptionPane.showConfirmDialog(this, goal.getGoalName() + " 목표를 삭제할까요?",
                "삭제", JOptionPane.OK_CANCEL_OPTION);
        if (answer != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            app.api.goalDelete(goal.getGoalId());
            reload();
        } catch (RuntimeException ex) {
            Alerts.error(this, ex);
        }
    }

    private GoalDTO selected() {
        int index = goalsTable.getSelectedRow();
        if (index < 0 || index >= goals.size()) {
            return null;
        }
        return goals.get(index);
    }

    private static String status(String value) {
        return switch (value == null ? "" : value) {
            case "ACTIVE" -> "진행";
            case "ACHIEVED" -> "달성";
            case "CANCELED" -> "취소";
            default -> value;
        };
    }
}
