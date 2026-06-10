package com.esunbank.seating.common.exception;

/**
 * 共用層：業務邏輯例外，由 GlobalExceptionHandler 轉為 400 回應。
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
