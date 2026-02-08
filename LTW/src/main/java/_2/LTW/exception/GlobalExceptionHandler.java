package _2.LTW.exception;

import _2.LTW.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global Exception Handler để xử lý tất cả exceptions trong ứng dụng
 * Tự động convert exception thành ErrorResponse chuẩn
 * 
 * @RestControllerAdvice: Áp dụng cho tất cả @RestController
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Hàm 1: Xử lý AppException (Custom Exception của ứng dụng)
     * Nhiệm vụ: Convert AppException thành ErrorResponse với ErrorCode
     */
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(
            AppException ex,
            HttpServletRequest request) {
        
        log.error("AppException: {} - {}", ex.getErrorCode().getCode(), ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .code(ex.getErrorCode().getCode())
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();
        
        return ResponseEntity
                .status(ex.getErrorCode().getStatusCode())
                .body(errorResponse);
    }

    /**
     * Hàm 2: Xử lý ValidationException (từ @Valid trong DTO)
     * Nhiệm vụ: Xử lý lỗi validation và trả về danh sách lỗi chi tiết
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        
        // Tạo map chứa các lỗi validation
        Map<String, String> validationErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            validationErrors.put(fieldName, errorMessage);
        });
        
        log.warn("Validation error: {} - {}", request.getRequestURI(), validationErrors);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .code(ErrorCode.BAD_REQUEST.getCode())
                .message(ErrorCode.BAD_REQUEST.getMessage())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .details(validationErrors) // Chi tiết các lỗi validation
                .build();
        
        return ResponseEntity
                .status(ErrorCode.BAD_REQUEST.getStatusCode())
                .body(errorResponse);
    }

    /**
     * Hàm 3: Xử lý IllegalArgumentException (thường từ service layer)
     * Nhiệm vụ: Convert IllegalArgumentException thành BAD_REQUEST response
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex,
            HttpServletRequest request) {
        
        log.warn("IllegalArgumentException: {} - {}", request.getRequestURI(), ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .code(ErrorCode.BAD_REQUEST.getCode())
                .message(ex.getMessage() != null ? ex.getMessage() : ErrorCode.BAD_REQUEST.getMessage())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();
        
        return ResponseEntity
                .status(ErrorCode.BAD_REQUEST.getStatusCode())
                .body(errorResponse);
    }

    /**
     * Hàm 4: Xử lý RuntimeException (catch-all cho các runtime exceptions)
     * Nhiệm vụ: Convert RuntimeException thành INTERNAL_SERVER_ERROR response
     * Lưu ý: Nên sử dụng AppException thay vì RuntimeException để có error code rõ ràng
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(
            RuntimeException ex,
            HttpServletRequest request) {
        
        log.error("RuntimeException: {} - {}", request.getRequestURI(), ex.getMessage(), ex);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .code(ErrorCode.INTERNAL_SERVER_ERROR.getCode())
                .message(ex.getMessage() != null ? ex.getMessage() : ErrorCode.INTERNAL_SERVER_ERROR.getMessage())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();
        
        return ResponseEntity
                .status(ErrorCode.INTERNAL_SERVER_ERROR.getStatusCode())
                .body(errorResponse);
    }

    /**
     * Hàm 5: Xử lý tất cả các Exception khác (catch-all)
     * Nhiệm vụ: Xử lý các exception không được handle bởi các hàm trên
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request) {
        
        log.error("Unexpected exception: {} - {}", request.getRequestURI(), ex.getMessage(), ex);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .code(ErrorCode.INTERNAL_SERVER_ERROR.getCode())
                .message(ErrorCode.INTERNAL_SERVER_ERROR.getMessage())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();
        
        return ResponseEntity
                .status(ErrorCode.INTERNAL_SERVER_ERROR.getStatusCode())
                .body(errorResponse);
    }
}
