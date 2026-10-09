package com.thonha.backend.common;

/** 400 - dữ liệu/ngữ cảnh yêu cầu không hợp lệ. */
public class BadRequestException extends ApiException {
    public BadRequestException(String message) {
        super(ErrorCode.INVALID_REQUEST, message);
    }
}
