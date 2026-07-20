package com.backend.exception;

import com.backend.dto.response.ApiResponse;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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

    // Bắt lỗi Validation (khi frontend gửi thiếu/sai data) — FIX: null-safe getFieldError()
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handlingValidation(MethodArgumentNotValidException exception) {
        // Fix: getFieldError() có thể null nếu là class-level constraint
        var fieldError = exception.getBindingResult().getFieldError();
        String rawMessage = fieldError != null
                ? fieldError.getDefaultMessage()
                : exception.getBindingResult().getAllErrors().stream()
                        .findFirst()
                        .map(e -> e.getDefaultMessage())
                        .orElse(ErrorCode.VALIDATION_ERROR.getMessage());

        ErrorCode errorCode = ErrorCode.VALIDATION_ERROR;
        String message = rawMessage;

        if (rawMessage != null) {
            try {
                ErrorCode match = ErrorCode.valueOf(rawMessage);
                errorCode = match;
                message = match.getMessage();
            } catch (IllegalArgumentException e) {
                // rawMessage là thông điệp tự viết, giữ nguyên làm message
            }
        }

        ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                .code(errorCode.getCode())
                .message(message)
                .build();

        return ResponseEntity.status(errorCode.getStatusCode()).body(apiResponse);
    }

    // Bắt lỗi path variable / request param sai kiểu (vd: /users/abc thay vì /users/1)
    @ExceptionHandler(value = MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handlingTypeMismatch(MethodArgumentTypeMismatchException exception) {
        String message = String.format("Tham số '%s' có giá trị '%s' không hợp lệ",
                exception.getName(), exception.getValue());
        ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                .code(ErrorCode.INVALID_REQUEST_FORMAT.getCode())
                .message(message)
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponse);
    }

    // Bắt lỗi enum valueOf() không hợp lệ (IllegalArgumentException)
    @ExceptionHandler(value = IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handlingIllegalArgument(IllegalArgumentException exception) {
        String message = exception.getMessage();
        if (message != null && message.contains("No enum constant")) {
            message = ErrorCode.INVALID_ENUM_VALUE.getMessage();
        }
        ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                .code(ErrorCode.INVALID_ENUM_VALUE.getCode())
                .message(message != null ? message : ErrorCode.INVALID_ENUM_VALUE.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponse);
    }

    // Bắt lỗi JSON body bị malformed hoặc thiếu
    @ExceptionHandler(value = HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handlingMessageNotReadable(HttpMessageNotReadableException exception) {
        ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                .code(ErrorCode.INVALID_REQUEST_FORMAT.getCode())
                .message("Dữ liệu gửi lên không đúng định dạng JSON hoặc thiếu trường bắt buộc")
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponse);
    }

    // Bắt lỗi query param bắt buộc bị thiếu
    @ExceptionHandler(value = MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handlingMissingParam(MissingServletRequestParameterException exception) {
        String message = String.format("Thiếu tham số bắt buộc: '%s'", exception.getParameterName());
        ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                .code(ErrorCode.VALIDATION_ERROR.getCode())
                .message(message)
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponse);
    }

    // Bắt lỗi URL không tồn tại
    @ExceptionHandler(value = NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handlingNoResource(NoResourceFoundException exception) {
        ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                .code(ErrorCode.RESOURCE_NOT_FOUND.getCode())
                .message("API endpoint không tồn tại: " + exception.getResourcePath())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponse);
    }

    // Bắt lỗi deleteById() khi ID không tồn tại (EmptyResultDataAccessException)
    @ExceptionHandler(value = EmptyResultDataAccessException.class)
    public ResponseEntity<ApiResponse<Void>> handlingEmptyResult(EmptyResultDataAccessException exception) {
        ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                .code(ErrorCode.RESOURCE_NOT_FOUND.getCode())
                .message(ErrorCode.RESOURCE_NOT_FOUND.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponse);
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

    // Bắt lỗi ràng buộc dữ liệu database (khoá ngoại, trùng lặp unique...)
    @ExceptionHandler(value = org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handlingDataIntegrityViolationException(org.springframework.dao.DataIntegrityViolationException exception) {
        exception.printStackTrace();
        String detail = "";
        if (exception.getCause() != null && exception.getCause().getMessage() != null) {
            String cause = exception.getCause().getMessage().toLowerCase();
            if (cause.contains("duplicate") || cause.contains("unique")) {
                detail = " (Dữ liệu bị trùng lặp)";
            } else if (cause.contains("foreign key") || cause.contains("fk_")) {
                detail = " (Dữ liệu đang được liên kết ở bảng khác)";
            } else if (cause.contains("not-null") || cause.contains("null value")) {
                detail = " (Thiếu giá trị bắt buộc)";
            }
        }
        ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                .code(ErrorCode.DATABASE_ERROR.getCode())
                .message("Vi phạm ràng buộc dữ liệu cơ sở dữ liệu" + detail)
                .build();
        return ResponseEntity.status(ErrorCode.DATABASE_ERROR.getStatusCode()).body(apiResponse);
    }

    // Bắt lỗi RuntimeException (lỗi nghiệp vụ ném bằng throw new RuntimeException("..."))
    @ExceptionHandler(value = RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handlingRuntimeException(RuntimeException exception) {
        exception.printStackTrace();

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
        exception.printStackTrace();

        ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                .code(ErrorCode.UNCATEGORIZED_EXCEPTION.getCode())
                .message(ErrorCode.UNCATEGORIZED_EXCEPTION.getMessage())
                .build();

        return ResponseEntity.status(ErrorCode.UNCATEGORIZED_EXCEPTION.getStatusCode()).body(apiResponse);
    }
}