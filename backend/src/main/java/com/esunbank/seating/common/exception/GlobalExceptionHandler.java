package com.esunbank.seating.common.exception;

import com.esunbank.seating.common.dto.ApiResponse;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 共用層：全域例外處理，統一錯誤回傳格式。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 業務例外 -> 400 */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.fail(ex.getMessage()));
    }

    /** 參數驗證失敗 (@Valid) -> 400 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(ApiResponse.fail(msg));
    }

    /**
     * 資料庫存取例外 (含 Stored Procedure RAISE EXCEPTION) -> 400。
     * 將 SP 拋出的訊息 (如 SEAT_OCCUPIED) 轉成友善提示。
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataAccess(DataAccessException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.fail(resolveDbMessage(ex)));
    }

    /** 其他未預期例外 -> 500 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleOther(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail("系統發生錯誤，請稍後再試"));
    }

    private String resolveDbMessage(DataAccessException ex) {
        String root = ex.getMostSpecificCause().getMessage();
        if (root == null) {
            return "資料庫存取失敗";
        }
        if (root.contains("SEAT_OCCUPIED")) {
            return "座位已被其他員工佔用，請重新整理後再試";
        }
        if (root.contains("SEAT_NOT_FOUND")) {
            return "指定的座位不存在";
        }
        if (root.contains("EMP_NOT_FOUND")) {
            return "指定的員工不存在";
        }
        if (root.contains("INVALID_EMP_ID")) {
            return "員編格式錯誤，需為 5 碼數字";
        }
        if (root.contains("uq_employee_seat")) {
            return "座位重複指派，請重新整理後再試";
        }
        return "資料庫存取失敗";
    }
}
