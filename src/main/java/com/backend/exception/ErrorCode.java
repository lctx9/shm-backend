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
    DATABASE_ERROR(1013, "Lỗi ràng buộc hoặc trùng lặp dữ liệu cơ sở dữ liệu", HttpStatus.CONFLICT),

    // ==========================================
    // MÃ LỖI XÁC THỰC & TÀI KHOẢN
    // ==========================================
    ACCOUNT_NOT_APPROVED(1014, "Tài khoản chưa được duyệt hoặc đã bị khóa", HttpStatus.FORBIDDEN),
    MAINTENANCE_MODE(1015, "Hệ thống đang bảo trì. Vui lòng quay lại sau.", HttpStatus.SERVICE_UNAVAILABLE),
    REGISTRATION_DISABLED(1016, "Hệ thống đang tạm đóng đăng ký tài khoản mới", HttpStatus.FORBIDDEN),
    OTP_REQUIRED(1017, "Vui lòng nhập mã OTP đã gửi qua email.", HttpStatus.BAD_REQUEST),
    OTP_NOT_REQUESTED(1018, "Bạn chưa yêu cầu mã OTP hoặc mã đã hết hạn.", HttpStatus.BAD_REQUEST),
    OTP_EXPIRED(1019, "Mã OTP đã hết hạn. Vui lòng gửi lại mã mới.", HttpStatus.BAD_REQUEST),
    OTP_INVALID(1020, "Mã OTP không đúng.", HttpStatus.BAD_REQUEST),
    EMAIL_SEND_FAILED(1021, "Không thể gửi email. Vui lòng thử lại sau.", HttpStatus.INTERNAL_SERVER_ERROR),

    // ==========================================
    // MÃ LỖI GIẢI ĐẤU & CẤU TRÚC
    // ==========================================
    EVENT_NOT_FOUND(1022, "Không tìm thấy giải đấu", HttpStatus.NOT_FOUND),
    TRACK_NOT_FOUND(1023, "Không tìm thấy hạng mục thi", HttpStatus.NOT_FOUND),
    MATRIX_NOT_FOUND(1024, "Không tìm thấy vòng đấu trong ma trận", HttpStatus.NOT_FOUND),
    PRIZE_NOT_FOUND(1025, "Không tìm thấy giải thưởng", HttpStatus.NOT_FOUND),

    // ==========================================
    // MÃ LỖI ĐĂNG KÝ ĐỘI & THAM GIA
    // ==========================================
    TEAM_FULL(1026, "Đội đã đạt tối đa 5 thành viên", HttpStatus.BAD_REQUEST),
    NOT_TEAM_LEADER(1027, "Chỉ Team Leader mới có quyền thực hiện thao tác này", HttpStatus.FORBIDDEN),
    JOIN_REQUEST_NOT_FOUND(1028, "Không tìm thấy yêu cầu tham gia đội", HttpStatus.NOT_FOUND),
    REGISTRATION_PERIOD_CLOSED(1029, "Thời gian đăng ký đội đã kết thúc hoặc chưa bắt đầu", HttpStatus.BAD_REQUEST),

    // ==========================================
    // MÃ LỖI NỘP BÀI & CHẤM ĐIỂM
    // ==========================================
    SUBMISSION_NOT_FOUND(1030, "Không tìm thấy bài nộp", HttpStatus.NOT_FOUND),
    SUBMISSION_DEADLINE_PASSED(1031, "Thời hạn nộp bài đã kết thúc", HttpStatus.BAD_REQUEST),
    SUBMISSION_ALREADY_EXISTS(1032, "Đội thi đã nộp bài cho vòng thi này rồi", HttpStatus.CONFLICT),
    JUDGE_NOT_ASSIGNED(1033, "Bạn chưa được phân công làm giám khảo cho vòng đấu này", HttpStatus.FORBIDDEN),
    EDIT_REASON_REQUIRED(1034, "Phải cung cấp lý do khi sửa điểm", HttpStatus.BAD_REQUEST),
    INVALID_SCORE_RANGE(1035, "Điểm số phải nằm trong khoảng từ 0 đến 100", HttpStatus.BAD_REQUEST),
    SCORE_REQUIRED(1036, "Phải nhập điểm chấm", HttpStatus.BAD_REQUEST),
    INVALID_CRITERIA_WEIGHT(1037, "Tổng trọng số tiêu chí phải lớn hơn 0", HttpStatus.BAD_REQUEST),
    INVALID_CRITERIA_SCORE(1038, "Điểm thành phần phải nằm trong khoảng từ 0 đến 100", HttpStatus.BAD_REQUEST),
    CRITERIA_SCORE_PARSE_FAILED(1039, "Không thể tính điểm từ cấu trúc tiêu chí", HttpStatus.BAD_REQUEST),

    // ==========================================
    // MÃ LỖI NHẬP LIỆU KHÔNG HỢP LỆ
    // ==========================================
    INVALID_ENUM_VALUE(1040, "Giá trị không hợp lệ cho trường dữ liệu này", HttpStatus.BAD_REQUEST),
    INVALID_REQUEST_FORMAT(1041, "Định dạng dữ liệu gửi lên không hợp lệ", HttpStatus.BAD_REQUEST),
    RESOURCE_NOT_FOUND(1042, "Không tìm thấy tài nguyên yêu cầu", HttpStatus.NOT_FOUND);

    private final int code;
    private final String message;
    private final HttpStatus statusCode;

    ErrorCode(int code, String message, HttpStatus statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }
}