package com.backend.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Lỗi hệ thống không xác định", HttpStatus.INTERNAL_SERVER_ERROR),
    USER_EXISTED(1001, "Email này đã được đăng ký", HttpStatus.BAD_REQUEST),
    USER_NOT_FOUND(1002, "Không tìm thấy người dùng", HttpStatus.NOT_FOUND),
    TEAM_EXISTED(1003, "Tên đội đã tồn tại", HttpStatus.BAD_REQUEST),
    UNAUTHENTICATED(1004, "Chưa xác thực người dùng", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1005, "Không có quyền truy cập", HttpStatus.FORBIDDEN),
    INVALID_PASSWORD(1006, "Mật khẩu không chính xác", HttpStatus.BAD_REQUEST),

    // ==========================================
    // MÃ LỖI BỔ SUNG CHO TÍNH NĂNG ĐỘI THI
    // ==========================================
    ALREADY_IN_TEAM(1007, "Bạn đã tham gia một đội rồi, không thể thực hiện thao tác này!", HttpStatus.BAD_REQUEST),
    TEAM_NOT_FOUND(1008, "Không tìm thấy đội thi yêu cầu", HttpStatus.NOT_FOUND),
    INVALID_JOIN_TYPE(1009, "Chế độ tham gia không đúng với thiết lập của đội (Public/Private)", HttpStatus.BAD_REQUEST),
    WRONG_JOIN_PASSWORD(1010, "Mật khẩu gia nhập đội thi không chính xác", HttpStatus.BAD_REQUEST),
    VALIDATION_ERROR(1011, "Dữ liệu đầu vào không hợp lệ", HttpStatus.BAD_REQUEST),
    BUSINESS_ERROR(1012, "Lỗi nghiệp vụ phát sinh", HttpStatus.BAD_REQUEST),
    DATABASE_ERROR(1013, "Lỗi ràng buộc hoặc trùng lặp dữ liệu cơ sở dữ liệu", HttpStatus.CONFLICT);

    private final int code;
    private final String message;
    private final HttpStatus statusCode;

    ErrorCode(int code, String message, HttpStatus statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }
}