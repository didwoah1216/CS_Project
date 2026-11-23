package com.chagok.client.ui;

import com.chagok.client.AppContext;
import com.chagok.client.util.Alerts;
import com.chagok.client.util.DigitsOnlyFilter;
import com.chagok.client.util.Money;
import com.chagok.common.dto.CategoryDTO;
import com.chagok.common.dto.TransactionDTO;
import com.google.gson.JsonObject;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.AbstractDocument;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** SCR-07 내역 검색. */
public class SearchFrame extends JFrame {
    private final AppContext app;
    private final JTextField from = new JTextField(10);
    private final JTextField to = new JTextField(10);
    private final JComboBox<CategoryDTO> categories = new JComboBox<>();
    private final JTextField minAmount = new JTextField(8);
    private final JTextField maxAmount = new JTextField(8);
    private final JTextField keyword = new JTextField(12);
    private final JLabel count = new JLabel(" ");
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"날짜", "구분", "카테고리", "유형", "메모", "금액"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);

    public SearchFrame(AppContext app, YearMonth month) {
        super("차곡 검색");
        this.app = app;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(860, 560);
        setLocationRelativeTo(null);
        from.setText(month.atDay(1).toString());
        to.setText(month.atEndOfMonth().toString());
        ((AbstractDocument) minAmount.getDocument()).setDocumentFilter(new DigitsOnlyFilter(9));
        ((AbstractDocument) maxAmount.getDocument()).setDocumentFilter(new DigitsOnlyFilter(9));
        categories.addItem(null);
        for (CategoryDTO category : app.categories) {
            categories.addItem(category);
        }
        categories.setRenderer((list, value, index, selected, focus) -> {
            JLabel label = new JLabel(value == null ? "전체" : value.getName());
            label.setOpaque(true);
            if (selected) {
                label.setBackground(list.getSelectionBackground());
                label.setForeground(list.getSelectionForeground());
            }
            return label;
        });
        add(filters(), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(count, BorderLayout.SOUTH);
        table.setRowHeight(26);
    }

    private JPanel filters() {
        JPanel grid = new JPanel(new GridLayout(2, 1, 4, 4));
        JPanel line1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        line1.add(new JLabel("기간"));
        line1.add(from);
        line1.add(new JLabel("~"));
        line1.add(to);
        line1.add(new JLabel("카테고리"));
        line1.add(categories);
        JPanel line2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        line2.add(new JLabel("금액"));
        line2.add(minAmount);
        line2.add(new JLabel("~"));
        line2.add(maxAmount);
        line2.add(new JLabel("키워드"));
        line2.add(keyword);
        JButton search = new JButton("검색");
        search.addActionListener(e -> search());
        line2.add(search);
        grid.add(line1);
        grid.add(line2);
        return grid;
    }

    private void search() {
        String fromText = from.getText().trim();
        String toText = to.getText().trim();
        if (fromText.isEmpty() != toText.isEmpty()) {
            Alerts.info(this, "시작일과 종료일을 함께 입력해 주세요");
            return;
        }
        Map<String, Object> body = new LinkedHashMap<>();
        if (!fromText.isEmpty()) {
            body.put("from", fromText);
            body.put("to", toText);
        }
        CategoryDTO category = (CategoryDTO) categories.getSelectedItem();
        if (category != null) {
            body.put("categoryId", category.getCategoryId());
        }
        if (!minAmount.getText().isBlank()) {
            body.put("minAmount", Integer.parseInt(minAmount.getText()));
        }
        if (!maxAmount.getText().isBlank()) {
            body.put("maxAmount", Integer.parseInt(maxAmount.getText()));
        }
        if (!keyword.getText().isBlank()) {
            body.put("keyword", keyword.getText().trim());
        }
        new SwingWorker<JsonObject, Void>() {
            @Override
            protected JsonObject doInBackground() {
                return app.api.search(body);
            }

            @Override
            protected void done() {
                try {
                    show(get());
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    if (cause instanceof Exception exception) {
                        Alerts.error(SearchFrame.this, exception);
                    }
                }
            }
        }.execute();
    }

    private void show(JsonObject body) {
        List<TransactionDTO> items = app.api.transactions(body.get("items"));
        model.setRowCount(0);
        for (TransactionDTO item : items) {
            String kind = "INCOME".equals(item.getType()) ? "수입" : "지출";
            String fixed = "FIXED".equals(item.getFixedType()) ? "고정" : "VARIABLE".equals(item.getFixedType()) ? "유동" : "";
            model.addRow(new Object[]{
                    item.getTransactionDate(),
                    kind,
                    item.getCategoryName(),
                    fixed,
                    item.getMemo() == null ? "" : item.getMemo(),
                    ("INCOME".equals(item.getType()) ? "+" : "-") + Money.comma(item.getAmount())
            });
        }
        count.setText("  " + body.get("count").getAsInt() + "건");
    }
}
