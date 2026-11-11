package com.chagok.client.ui;

import com.chagok.client.AppContext;
import com.chagok.client.util.Alerts;
import com.chagok.client.util.DigitsOnlyFilter;
import com.chagok.common.dto.CategoryDTO;
import com.chagok.common.dto.TransactionDTO;
import com.chagok.common.protocol.BizException;
import com.chagok.common.util.InputValidator;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.border.Border;
import javax.swing.text.AbstractDocument;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

public class TransactionDialog extends JDialog {
    private static final String[] REPEAT_LABELS = {"없음", "매일", "매주", "매월", "매년"};
    private static final String[] REPEAT_VALUES = {"NONE", "DAILY", "WEEKLY", "MONTHLY", "YEARLY"};

    private final AppContext app;
    private final TransactionDTO editing;
    private final LocalDate defaultDate;
    private final JRadioButton expense = new JRadioButton("지출", true);
    private final JRadioButton income = new JRadioButton("수입");
    private final JRadioButton fixed = new JRadioButton("고정지출");
    private final JRadioButton variable = new JRadioButton("유동지출");
    private final JTextField amount = new JTextField(12);
    private final JTextField date = new JTextField(12);
    private final JComboBox<CategoryDTO> categories = new JComboBox<>();
    private final JComboBox<String> repeat = new JComboBox<>(REPEAT_LABELS);
    private final JTextField memo = new JTextField(18);
    private final Border normalBorder = new JTextField().getBorder();
    private boolean applying;
    private boolean saved;

    public TransactionDialog(java.awt.Frame owner, AppContext app, TransactionDTO editing, LocalDate defaultDate) {
        super(owner, editing == null ? "내역 추가" : "내역 수정", true);
        this.app = app;
        this.editing = editing;
        this.defaultDate = defaultDate;
        setLayout(new BorderLayout(8, 8));
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        add(form(), BorderLayout.CENTER);
        add(buttons(), BorderLayout.SOUTH);
        wire();
        fill();
        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(owner);
        getRootPane().setDefaultButton((JButton) ((JPanel) getContentPane().getComponent(1)).getComponent(0));
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");
        getRootPane().getActionMap().put("cancel", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
    }

    public boolean showDialog() {
        setVisible(true);
        return saved;
    }

    private JPanel form() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;
        ButtonGroup kind = new ButtonGroup();
        kind.add(expense);
        kind.add(income);
        ButtonGroup fix = new ButtonGroup();
        fix.add(fixed);
        fix.add(variable);
        ((AbstractDocument) amount.getDocument()).setDocumentFilter(new DigitsOnlyFilter(9));
        int row = 0;
        row = line(panel, c, row, "구분", rowOf(expense, income));
        row = line(panel, c, row, "고정/유동", rowOf(fixed, variable));
        row = line(panel, c, row, "금액", amount);
        row = line(panel, c, row, "날짜", date);
        row = line(panel, c, row, "카테고리", categories);
        row = line(panel, c, row, "반복", repeat);
        line(panel, c, row, "메모", memo);
        categories.setRenderer((list, value, index, selected, focus) -> {
            JLabel label = new JLabel(value == null ? " " : value.getName());
            label.setOpaque(true);
            if (selected) {
                label.setBackground(list.getSelectionBackground());
                label.setForeground(list.getSelectionForeground());
            }
            return label;
        });
        return panel;
    }

    private JPanel buttons() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton save = new JButton("저장");
        JButton cancel = new JButton("취소");
        save.addActionListener(e -> save());
        cancel.addActionListener(e -> dispose());
        panel.add(save);
        panel.add(cancel);
        return panel;
    }

    private void wire() {
        expense.addActionListener(e -> reloadCategories(false));
        income.addActionListener(e -> reloadCategories(false));
        categories.addActionListener(e -> applyDefaultFixed());
    }

    private void fill() {
        date.setText((defaultDate == null ? com.chagok.common.util.DateUtil.today() : defaultDate).toString());
        if (editing != null) {
            applying = true;
            boolean isIncome = "INCOME".equals(editing.getType());
            income.setSelected(isIncome);
            expense.setSelected(!isIncome);
            amount.setText(Integer.toString(editing.getAmount()));
            date.setText(editing.getTransactionDate().toString());
            memo.setText(editing.getMemo() == null ? "" : editing.getMemo());
            selectRepeat(editing.getRepeatCycle());
            reloadCategories(true);
            selectCategory(editing.getCategoryId());
            if ("FIXED".equals(editing.getFixedType())) {
                fixed.setSelected(true);
            } else if ("VARIABLE".equals(editing.getFixedType())) {
                variable.setSelected(true);
            }
            applying = false;
        } else {
            reloadCategories(false);
        }
    }

    private void reloadCategories(boolean keepFixed) {
        applying = true;
        categories.removeAllItems();
        String type = income.isSelected() ? "INCOME" : "EXPENSE";
        for (CategoryDTO category : app.categories) {
            if (type.equals(category.getType())) {
                categories.addItem(category);
            }
        }
        boolean expenseSelected = expense.isSelected();
        fixed.setEnabled(expenseSelected);
        variable.setEnabled(expenseSelected);
        applying = false;
        if (!expenseSelected) {
            fixed.setSelected(false);
            variable.setSelected(false);
        } else if (!keepFixed) {
            applyDefaultFixed();
        }
    }

    private void applyDefaultFixed() {
        if (applying || !expense.isSelected()) {
            return;
        }
        CategoryDTO category = (CategoryDTO) categories.getSelectedItem();
        if (category == null) {
            return;
        }
        if ("FIXED".equals(category.getDefaultFixedType())) {
            fixed.setSelected(true);
        } else if ("VARIABLE".equals(category.getDefaultFixedType())) {
            variable.setSelected(true);
        }
    }

    private void save() {
        clearBorders();
        CategoryDTO category = (CategoryDTO) categories.getSelectedItem();
        String type = income.isSelected() ? "INCOME" : "EXPENSE";
        String fixedType = null;
        boolean fixedPresent = false;
        if (expense.isSelected() && (fixed.isSelected() || variable.isSelected())) {
            fixedType = fixed.isSelected() ? "FIXED" : "VARIABLE";
            fixedPresent = true;
        }
        try {
            InputValidator.requireCategory(category == null ? null : category.getCategoryId());
            String amountText = amount.getText().trim();
            Integer amountValue = amountText.isEmpty() ? null : Integer.valueOf(amountText);
            InputValidator.requireAmount(amountValue, !amountText.isEmpty());
            InputValidator.requireDate(date.getText(), !date.getText().isBlank());
            InputValidator.fixedTypeFor(type, fixedType, fixedPresent);
            InputValidator.memoOrNull(memo.getText());
            Map<String, Object> body = new LinkedHashMap<>();
            if (editing != null) {
                body.put("transactionId", editing.getTransactionId());
            }
            body.put("categoryId", category.getCategoryId());
            body.put("amount", amountValue);
            body.put("transactionDate", date.getText().trim());
            if (fixedPresent) {
                body.put("fixedType", fixedType);
            }
            body.put("repeatCycle", REPEAT_VALUES[repeat.getSelectedIndex()]);
            if (!memo.getText().isBlank()) {
                body.put("memo", memo.getText().trim());
            }
            if (editing == null) {
                app.api.addTransaction(body);
            } else {
                app.api.updateTransaction(body);
            }
            saved = true;
            dispose();
        } catch (BizException ex) {
            mark(ex.getMessage());
            Alerts.error(this, ex);
        } catch (RuntimeException ex) {
            Alerts.error(this, ex);
        }
    }

    private void mark(String message) {
        if (message.contains("금액")) {
            amount.setBorder(BorderFactory.createLineBorder(Color.RED, 2));
        }
        if (message.contains("날짜")) {
            date.setBorder(BorderFactory.createLineBorder(Color.RED, 2));
        }
        if (message.contains("카테고리")) {
            categories.setBorder(BorderFactory.createLineBorder(Color.RED, 2));
        }
        if (message.contains("고정") || message.contains("유동")) {
            fixed.setForeground(Color.RED);
            variable.setForeground(Color.RED);
        }
        if (message.contains("메모")) {
            memo.setBorder(BorderFactory.createLineBorder(Color.RED, 2));
        }
    }

    private void clearBorders() {
        amount.setBorder(normalBorder);
        date.setBorder(normalBorder);
        memo.setBorder(normalBorder);
        categories.setBorder(normalBorder);
        fixed.setForeground(null);
        variable.setForeground(null);
    }

    private void selectCategory(int categoryId) {
        for (int i = 0; i < categories.getItemCount(); i++) {
            if (categories.getItemAt(i).getCategoryId() == categoryId) {
                categories.setSelectedIndex(i);
                return;
            }
        }
    }

    private void selectRepeat(String value) {
        for (int i = 0; i < REPEAT_VALUES.length; i++) {
            if (REPEAT_VALUES[i].equals(value)) {
                repeat.setSelectedIndex(i);
                return;
            }
        }
    }

    private static int line(JPanel panel, GridBagConstraints c, int row, String label, java.awt.Component field) {
        c.gridx = 0;
        c.gridy = row;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        panel.add(new JLabel(label), c);
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(field, c);
        return row + 1;
    }

    private static JPanel rowOf(java.awt.Component left, java.awt.Component right) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        panel.add(left);
        panel.add(right);
        return panel;
    }
}
