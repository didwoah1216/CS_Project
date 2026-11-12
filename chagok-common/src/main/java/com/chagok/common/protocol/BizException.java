package com.chagok.common.protocol;

public class BizException extends RuntimeException {
    private final ErrorCode code;

    public BizException(ErrorCode code) {
        this(code, code.getDefaultMessage());
    }

    public BizException(ErrorCode code, String message) {
        super(message == null || message.isBlank() ? code.getDefaultMessage() : message);
        this.code = code;
    }

    public ErrorCode getCode() {
        return code;
    }
}
