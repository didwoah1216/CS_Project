package com.chagok.client.ui;

import com.chagok.client.AppContext;
import com.chagok.client.util.Alerts;
import com.chagok.common.dto.SettingDTO;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;

public class SettingsDialog extends JDialog {
    private final AppContext app;
    private final Runnable onChanged;
    private final JCheckBox warnEnabled = new JCheckBox("과소비 경고 사용", true);
    private final JSpinner warnRatio = spinner(10, 100, 70);
    private final JSpinner peerGap = spinner(0, 50, 10);
    private final JSpinner budgetAlert = spinner(50, 100, 80);
    private final JCheckBox sameGender = new JCheckBox("같은 성별만 비교");

    public SettingsDialog(Frame owner, AppContext app, Runnable onChanged) {
        super(owner, "설정", true);
        this.app = app;
        this.onChanged = onChanged;
        setLayout(new BorderLayout(8, 8));
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(warnEnabled);
        form.add(new JLabel(""));
        form.add(new JLabel("경고 기준 (%)"));
        form.add(warnRatio);
        form.add(new JLabel("또래 허용 폭 (%p)"));
        form.add(peerGap);
        form.add(new JLabel("예산 알림 기준 (%)"));
        form.add(budgetAlert);
        form.add(sameGender);
        form.add(new JLabel(""));
        add(form, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton reset = new JButton("더미 데이터 초기화");
        JButton save = new JButton("저장");
        JButton close = new JButton("닫기");
        reset.addActionListener(e -> resetDummy());
        save.addActionListener(e -> save());
        close.addActionListener(e -> dispose());
        buttons.add(reset);
        buttons.add(save);
        buttons.add(close);
        add(buttons, BorderLayout.SOUTH);
        load();
        pack();
        setLocationRelativeTo(owner);
    }

    private void load() {
        try {
            SettingDTO setting = app.api.setting();
            warnEnabled.setSelected(setting.isWarnEnabled());
            warnRatio.setValue(setting.getWarnRatio());
            peerGap.setValue(setting.getPeerGapRatio());
            budgetAlert.setValue(setting.getBudgetAlertRatio());
            sameGender.setSelected(setting.isCompareSameGender());
        } catch (RuntimeException ex) {
            Alerts.error(this, ex);
        }
    }

    private void save() {
        try {
            SettingDTO setting = new SettingDTO();
            setting.setWarnEnabled(warnEnabled.isSelected());
            setting.setWarnRatio(((Number) warnRatio.getValue()).intValue());
            setting.setPeerGapRatio(((Number) peerGap.getValue()).intValue());
            setting.setBudgetAlertRatio(((Number) budgetAlert.getValue()).intValue());
            setting.setCompareSameGender(sameGender.isSelected());
            app.api.updateSetting(setting);
            onChanged.run();
            dispose();
        } catch (RuntimeException ex) {
            Alerts.error(this, ex);
        }
    }

    private void resetDummy() {
        int answer = JOptionPane.showConfirmDialog(this,
                "test01 데이터를 더미 초기 상태(내역 2,284건)로 되돌릴까요?",
                "초기화", JOptionPane.OK_CANCEL_OPTION);
        if (answer != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            app.api.resetDummy();
            onChanged.run();
            Alerts.info(this, "더미 데이터를 초기화했습니다");
            dispose();
        } catch (RuntimeException ex) {
            Alerts.error(this, ex);
        }
    }

    private static JSpinner spinner(int min, int max, int value) {
        return new JSpinner(new SpinnerNumberModel(value, min, max, 1));
    }
}
