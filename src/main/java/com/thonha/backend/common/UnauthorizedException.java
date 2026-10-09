package com.thonha.backend.common;

/** 401 - chưa đăng nhập hoặc phiên không hợp lệ. */
public class UnauthorizedException extends ApiException {
    public UnauthorizedException(String message) {
        super(ErrorCode.UNAUTHORIZED, message);
    }
}
