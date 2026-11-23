package com.chagok.server.service;

import com.chagok.common.dto.TransactionDTO;
import com.chagok.common.util.DateUtil;
import com.chagok.common.util.RecurringPolicy;
import com.chagok.server.dao.TransactionDAO;
import com.chagok.server.util.DbException;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RecurringService {
    private final TransactionDAO transactionDAO;

    public RecurringService(TransactionDAO transactionDAO) {
        this.transactionDAO = transactionDAO;
    }

    public Map<String, Object> run(int userId, String ym) {
        YearMonth month = DateUtil.parseYm(ym);
        LocalDate latest = DateUtil.today().plusYears(1);
        int created = 0;
        int skipped = 0;
        List<TransactionDTO> items = new ArrayList<>();
        for (TransactionDTO template : transactionDAO.templates(userId)) {
            for (LocalDate date : RecurringPolicy.occurrences(
                    template.getTransactionDate(), template.getRepeatCycle(), month)) {
                if (date.isBefore(DateUtil.MIN_DATE) || date.isAfter(latest)) {
                    skipped++;
                    continue;
                }
                if (transactionDAO.existsChild(template.getTransactionId(), date)) {
                    skipped++;
                    continue;
                }
                try {
                    int id = transactionDAO.insertAuto(userId, template, date);
                    created++;
                    TransactionDTO saved = transactionDAO.findById(id);
                    if (saved != null) {
                        items.add(saved);
                    }
                } catch (DbException ex) {
                    if (duplicate(ex)) {
                        skipped++;
                    } else {
                        throw ex;
                    }
                }
            }
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("created", created);
        body.put("skipped", skipped);
        body.put("items", items);
        return body;
    }

    private static boolean duplicate(Throwable error) {
        Throwable cursor = error;
        while (cursor != null) {
            if (cursor instanceof SQLException sql && sql.getErrorCode() == 1062) {
                return true;
            }
            cursor = cursor.getCause();
        }
        return false;
    }
}
