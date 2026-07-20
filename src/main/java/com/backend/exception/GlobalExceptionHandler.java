package com.backend.exception;

import com.backend.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    // Bắt các lỗi do mình chủ động throw (AppException)
    @ExceptionHandler(value = AppException.class)
    public ResponseEntity<ApiResponse<Void>> handlingAppException(AppException exception) {
        ErrorCode errorCode = exception.getErrorCode();

        ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();

        return ResponseEntity.status(errorCode.getStatusCode()).body(apiResponse);
    }

    // Bắt lỗi RuntimeException (bao gồm lỗi nghiệp vụ ném bằng throw new RuntimeException("...") )
    @ExceptionHandler(value = RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handlingRuntimeException(RuntimeException exception) {
        exception.printStackTrace(); // Log lỗi ra console để debug
        
        String message = exception.getMessage();
        ErrorCode errorCode = ErrorCode.BUSINESS_ERROR;
        
        // Nhận diện lỗi hệ thống thực sự (NullPointer, IndexOutOfBounds, v.v.)
        if (exception instanceof NullPointerException 
                || exception instanceof IndexOutOfBoundsException 
                || exception instanceof ClassCastException
                || message == null 
                || message.trim().isEmpty()) {
            message = ErrorCode.UNCATEGORIZED_EXCEPTION.getMessage();
            errorCode = ErrorCode.UNCATEGORIZED_EXCEPTION;
        }

        ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                .code(errorCode.getCode())
                .message(message)
                .build();

        return ResponseEntity.status(errorCode.getStatusCode()).body(apiResponse);
    }

    // Bắt lỗi hệ thống (những lỗi không lường trước cấp cao hơn)
    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<ApiResponse<Void>> handlingException(Exception exception) {
        exception.printStackTrace(); // Log lỗi ra console
        
        ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                .code(ErrorCode.UNCATEGORIZED_EXCEPTION.getCode())
                .message(ErrorCode.UNCATEGORIZED_EXCEPTION.getMessage())
                .build();

        return ResponseEntity.status(ErrorCode.UNCATEGORIZED_EXCEPTION.getStatusCode()).body(apiResponse);
    }

    // Bắt lỗi Validation (khi frontend gửi thiếu/sai data)
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handlingValidation(MethodArgumentNotValidException exception) {
        String enumKey = exception.getFieldError().getDefaultMessage();
        ErrorCode errorCode = ErrorCode.VALIDATION_ERROR;
        String message = enumKey;

        try {
            ErrorCode match = ErrorCode.valueOf(enumKey);
            errorCode = match;
            message = match.getMessage();
        } catch (IllegalArgumentException e) {
            // enumKey là thông điệp tự viết, giữ nguyên làm message
        }

        ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                .code(errorCode.getCode())
                .message(message)
                .build();

        return ResponseEntity.status(errorCode.getStatusCode()).body(apiResponse);
    }

    // Bắt lỗi phân quyền Spring Security (Access Denied)
    @ExceptionHandler(value = org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handlingAccessDeniedException(org.springframework.security.access.AccessDeniedException exception) {
        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;
        ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();

        return ResponseEntity.status(errorCode.getStatusCode()).body(apiResponse);
    }

    // Bắt lỗi ràng buộc dữ liệu database (ví dụ: khoá ngoại, trùng lặp unique)
    @ExceptionHandler(value = org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handlingDataIntegrityViolationException(org.springframework.dao.DataIntegrityViolationException exception) {
        exception.printStackTrace();
        ErrorCode errorCode = ErrorCode.DATABASE_ERROR;
        ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                .code(errorCode.getCode())
                .message("Dữ liệu đang được liên kết ở bảng khác hoặc vi phạm ràng buộc dữ liệu cơ sở dữ liệu")
                .build();

        return ResponseEntity.status(errorCode.getStatusCode()).body(apiResponse);
    }
}