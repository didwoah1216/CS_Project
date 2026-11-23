package com.chagok.server.service;

import com.chagok.common.dto.TransactionDTO;
import com.chagok.common.util.CsvWriter;
import com.chagok.common.util.DateUtil;
import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ExportService {
    private final TransactionService transactionService;

    public ExportService(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    public Map<String, Object> export(int userId, JsonObject data) {
        Map<String, Object> found = transactionService.search(userId, data);
        @SuppressWarnings("unchecked")
        List<TransactionDTO> items = (List<TransactionDTO>) found.get("items");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("filename", "chagok-" + DateUtil.today() + ".csv");
        body.put("csv", CsvWriter.transactions(items));
        body.put("count", items.size());
        return body;
    }
}
