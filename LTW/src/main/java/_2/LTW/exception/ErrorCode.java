package _2.LTW.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

/**
 * Enum định nghĩa các Error Code của ứng dụng
 * Mỗi error code có: code, message, statusCode
 * 
 * Quy tắc đặt code:
 * - 1xxx: General errors (BAD_REQUEST, NOT_FOUND, ...)
 * - 2xxx: Authentication & Authorization errors
 * - 3xxx: Business logic errors (User, Order, ...)
 * - 4xxx: Validation errors
 * - 5xxx: External service errors
 */
@Getter
public enum ErrorCode {

    // ========== General Errors (1xxx) ==========
    BAD_REQUEST(1001, "Yêu cầu không hợp lệ", HttpStatus.BAD_REQUEST),
    INTERNAL_SERVER_ERROR(1002, "Lỗi server", HttpStatus.INTERNAL_SERVER_ERROR),
    NOT_FOUND(1003, "Không tìm thấy", HttpStatus.NOT_FOUND),
    CONFLICT(1004, "Trùng lặp", HttpStatus.CONFLICT),
    METHOD_NOT_ALLOWED(1005, "Phương thức không được phép", HttpStatus.METHOD_NOT_ALLOWED),
    UNSUPPORTED_MEDIA_TYPE(1006, "Định dạng không được hỗ trợ", HttpStatus.UNSUPPORTED_MEDIA_TYPE),

    // ========== Authentication & Authorization Errors (2xxx) ==========
    UNAUTHENTICATED(2001, "Không đăng nhập", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(2002, "Không có quyền", HttpStatus.FORBIDDEN),
    INVALID_TOKEN(2003, "Token không hợp lệ", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED(2004, "Token đã hết hạn", HttpStatus.UNAUTHORIZED),
    INVALID_CREDENTIALS(2005, "Thông tin đăng nhập không đúng", HttpStatus.UNAUTHORIZED),

    // ========== User Errors (3xxx) ==========
    USER_NOT_FOUND(3001, "Không tìm thấy người dùng", HttpStatus.NOT_FOUND),
    USER_ALREADY_EXISTS(3002, "Người dùng đã tồn tại", HttpStatus.CONFLICT),
    USERNAME_ALREADY_EXISTS(3003, "Tên đăng nhập đã tồn tại", HttpStatus.CONFLICT),
    EMAIL_ALREADY_EXISTS(3004, "Email đã tồn tại", HttpStatus.CONFLICT),
    INVALID_PASSWORD(3005, "Mật khẩu không đúng", HttpStatus.BAD_REQUEST),
    ROLE_NOT_FOUND(3006, "Không tìm thấy vai trò", HttpStatus.NOT_FOUND),

    PET_NOT_FOUND(4101, "Không tìm thấy thú cưng", HttpStatus.NOT_FOUND),
    PET_OWNER_NOT_FOUND(4102, "Không tìm thấy chủ thú cưng", HttpStatus.NOT_FOUND),

    // ========== Care Booking Errors (5xxx) ==========
    CARE_BOOKING_NOT_FOUND(5001, "Không tìm thấy lịch đặt chăm sóc", HttpStatus.NOT_FOUND),
    CARE_BOOKING_CANNOT_UPDATE(5002, "Không thể cập nhật lịch đặt đã xử lý", HttpStatus.BAD_REQUEST),
    CARE_BOOKING_INVALID_STATUS(5003, "Trạng thái không hợp lệ", HttpStatus.BAD_REQUEST),
    ;


    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }

    private final int code;
    private final String message;
    private final HttpStatusCode statusCode;

    public AppException toException() {
        return new AppException(this);
    }

    public AppException toException(String message) {
        return new AppException(this, message);
    }
}
