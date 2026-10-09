package com.thonha.backend.common;

/** 404 - không tìm thấy tài nguyên. */
public class NotFoundException extends ApiException {
    public NotFoundException(String message) {
        super(ErrorCode.NOT_FOUND, message);
    }
}
