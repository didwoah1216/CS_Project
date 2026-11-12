package com.chagok.common.protocol;

import com.google.gson.JsonObject;

public class Request {
    private long reqId;
    private String cmdRaw;
    private Command cmd;
    private JsonObject data;

    public long getReqId() {
        return reqId;
    }

    public void setReqId(long reqId) {
        this.reqId = reqId;
    }

    public String getCmdRaw() {
        return cmdRaw;
    }

    public void setCmdRaw(String cmdRaw) {
        this.cmdRaw = cmdRaw;
    }

    public Command getCmd() {
        return cmd;
    }

    public void setCmd(Command cmd) {
        this.cmd = cmd;
    }

    public JsonObject getData() {
        return data == null ? new JsonObject() : data;
    }

    public void setData(JsonObject data) {
        this.data = data;
    }

    public String getString(String field) {
        JsonObject body = getData();
        if (!body.has(field) || body.get(field).isJsonNull()) {
            return null;
        }
        return body.get(field).getAsString();
    }

    public Boolean getBool(String field) {
        JsonObject body = getData();
        if (!body.has(field) || body.get(field).isJsonNull()) {
            return null;
        }
        return body.get(field).getAsBoolean();
    }
}
