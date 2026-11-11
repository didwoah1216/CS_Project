package com.chagok.common.util;

import com.chagok.common.protocol.BizException;
import com.chagok.common.protocol.ErrorCode;

public final class ComparePolicy {
    public static final int MIN_SAMPLE = 3;

    private ComparePolicy() {
    }

    public static void requireSample(int sampleSize) {
        if (sampleSize < MIN_SAMPLE) {
            throw new BizException(ErrorCode.E401, ErrorCode.E401.getDefaultMessage());
        }
    }
}
