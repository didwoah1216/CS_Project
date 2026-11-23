package com.chagok.common.util;

import com.chagok.common.dto.TransactionDTO;

import java.util.List;

/** FR-17 검색 결과를 엑셀에서 열 수 있는 CSV(UTF-8 BOM, CRLF)로 만든다. */
public final class CsvWriter {
    private CsvWriter() {
    }

    public static String transactions(List<TransactionDTO> items) {
        StringBuilder csv = new StringBuilder();
        csv.append('\uFEFF');
        csv.append("날짜,구분,카테고리,유형,금액,메모,반복,자동생성\r\n");
        if (items != null) {
            for (TransactionDTO item : items) {
                csv.append(cell(item.getTransactionDate() == null ? "" : item.getTransactionDate().toString())).append(',');
                csv.append(cell("INCOME".equals(item.getType()) ? "수입" : "지출")).append(',');
                csv.append(cell(item.getCategoryName())).append(',');
                csv.append(cell(fixed(item.getFixedType()))).append(',');
                csv.append(item.getAmount()).append(',');
                csv.append(cell(item.getMemo())).append(',');
                csv.append(cell(item.getRepeatCycle())).append(',');
                csv.append(item.isAuto() ? "Y" : "N").append("\r\n");
            }
        }
        return csv.toString();
    }

    static String cell(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        boolean quote = value.indexOf(',') >= 0 || value.indexOf('"') >= 0
                || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0;
        String escaped = value.replace("\"", "\"\"");
        return quote ? "\"" + escaped + "\"" : escaped;
    }

    private static String fixed(String fixedType) {
        if ("FIXED".equals(fixedType)) {
            return "고정";
        }
        if ("VARIABLE".equals(fixedType)) {
            return "유동";
        }
        return "";
    }
}
