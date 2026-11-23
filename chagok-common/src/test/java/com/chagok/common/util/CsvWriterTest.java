package com.chagok.common.util;

import com.chagok.common.dto.TransactionDTO;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvWriterTest {

    @Test
    void quotesCommasAndQuotesAndKeepsBom() {
        TransactionDTO item = new TransactionDTO();
        item.setTransactionDate(LocalDate.of(2026, 10, 3));
        item.setType("EXPENSE");
        item.setCategoryName("식비");
        item.setFixedType("VARIABLE");
        item.setAmount(8500);
        item.setMemo("학식, \"라면\"");
        item.setRepeatCycle("NONE");
        item.setAuto(false);
        String csv = CsvWriter.transactions(List.of(item));
        assertTrue(csv.startsWith("\uFEFF날짜,구분,카테고리,유형,금액,메모,반복,자동생성\r\n"));
        assertTrue(csv.contains("2026-10-03,지출,식비,유동,8500,\"학식, \"\"라면\"\"\",NONE,N\r\n"));
        assertEquals("", CsvWriter.cell(null));
    }
}
