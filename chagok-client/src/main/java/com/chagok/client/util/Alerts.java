package com.chagok.client.util;

import com.chagok.client.net.ApiException;
import com.chagok.common.protocol.ErrorCode;

import javax.swing.JOptionPane;
import java.awt.Component;

public final class Alerts {
    private Alerts() {
    }

    public static void error(Component parent, Exception ex) {
        String message = ex.getMessage() == null ? "요청을 처리하지 못했습니다" : ex.getMessage();
        if (ex instanceof ApiException api) {
            if (api.getCode() == ErrorCode.E301 || api.getCode() == ErrorCode.E302) {
                message = "삭제되었거나 접근할 수 없는 내역입니다";
            } else if (api.getCode() == ErrorCode.E401) {
                message = "비교할 같은 연령대 사용자가 부족함";
            } else if (api.getCode() == ErrorCode.E500 || api.getCode() == ErrorCode.E501) {
                message = "서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요";
            }
        }
        JOptionPane.showMessageDialog(parent, message, "차곡", JOptionPane.WARNING_MESSAGE);
    }

    public static void info(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "차곡", JOptionPane.INFORMATION_MESSAGE);
    }
}
