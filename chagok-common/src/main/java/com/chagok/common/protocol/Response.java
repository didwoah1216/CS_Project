package com.chagok.common.protocol;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

public class Response {
    private long reqId;
    private String cmd;
    private String status;
    private String code;
    private String message;
    private JsonElement data;

    public static Response ok(Request req, Object payload) {
        return ok(req, payload, "");
    }

    public static Response ok(Request req, Object payload, String message) {
        Response response = base(req);
        response.status = "OK";
        response.code = ErrorCode.S000.getCode();
        response.message = message == null ? "" : message;
        response.data = payload == null ? JsonNull.INSTANCE : JsonUtil.toTree(payload);
        return response;
    }

    public static Response error(Request req, ErrorCode code) {
        return error(req, code, code.getDefaultMessage());
    }

    public static Response error(Request req, ErrorCode code, String message) {
        Response response = base(req);
        response.status = "ERROR";
        response.code = code.getCode();
        response.message = message == null || message.isBlank() ? code.getDefaultMessage() : message;
        response.data = JsonNull.INSTANCE;
        return response;
    }

    public static Response errorRaw(long reqId, String cmd, ErrorCode code, String message) {
        Response response = new Response();
        response.reqId = reqId;
        response.cmd = cmd;
        response.status = "ERROR";
        response.code = code.getCode();
        response.message = message == null || message.isBlank() ? code.getDefaultMessage() : message;
        response.data = JsonNull.INSTANCE;
        return response;
    }

    private static Response base(Request req) {
        Response response = new Response();
        if (req != null) {
            response.reqId = req.getReqId();
            response.cmd = req.getCmdRaw();
        }
        return response;
    }

    public long getReqId() {
        return reqId;
    }

    public String getCmd() {
        return cmd;
    }

    public String getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public JsonElement getData() {
        return data;
    }
}
