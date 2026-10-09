package com.thonha.backend.common;

/** 403 - đã đăng nhập nhưng không có quyền với tài nguyên này (khác 401: không làm FE tự đăng xuất). */
public class ForbiddenException extends ApiException {
    public ForbiddenException(String message) {
        super(ErrorCode.FORBIDDEN, message);
    }
}
