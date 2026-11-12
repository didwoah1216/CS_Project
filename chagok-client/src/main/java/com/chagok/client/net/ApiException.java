package com.chagok.client.net;

import com.chagok.common.protocol.ErrorCode;

public class ApiException extends RuntimeException {
    private final ErrorCode code;

    public ApiException(ErrorCode code, String message) {
        super(message == null || message.isBlank() ? code.getDefaultMessage() : message);
        this.code = code;
    }

    public ErrorCode getCode() {
        return code;
    }
}
