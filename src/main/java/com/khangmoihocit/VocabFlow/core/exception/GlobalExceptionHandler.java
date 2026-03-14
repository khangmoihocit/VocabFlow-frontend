package com.khangmoihocit.VocabFlow.core.exception;

import com.khangmoihocit.VocabFlow.core.enums.ErrorCode;
import com.khangmoihocit.VocabFlow.core.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
@Slf4j(topic = "GlobalExceptionHandler")
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    ResponseEntity<ApiResponse<?>> handlingRuntimeException(RuntimeException exception) {
        log.error("Exception: ", exception);
        ErrorCode errorCode = ErrorCode.UNCATEGORIZED_EXCEPTION;
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.error(errorCode));
    }

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<?>> handleAppException(AppException exception){
        ErrorCode errorCode = exception.getErrorCode();
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.error(errorCode));
    }

    @ExceptionHandler(ValidTokenException.class)
    public ResponseEntity<?> handleAppException(ValidTokenException exception, @NotNull HttpServletRequest request){
        String message = exception.getMessage();
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", System.currentTimeMillis());
        errorResponse.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        errorResponse.put("error", "Xác thực không thành công");
        errorResponse.put("message", message);
        errorResponse.put("path", request.getRequestURL());
        return ResponseEntity
                .status(HttpServletResponse.SC_UNAUTHORIZED)
                .body(errorResponse);
    }

    //bắt lỗi từ @Valid
    //return 1 list error
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<?>> handleValidException(MethodArgumentNotValidException exception){

        Map<String, String> errors = new HashMap<>();

        exception.getBindingResult().getFieldErrors().forEach(error -> {
            errors.put(error.getField(), error.getDefaultMessage());
        });

        ErrorCode errorCode = ErrorCode.VALIDATION_ERROR;

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.error(errorCode, errors));
    }

    //username sai hoặc password sai
    //xảy ra bên trong AuthenticationManager hoặc DaoAuthenticationProvider.
    @ExceptionHandler(org.springframework.security.authentication.BadCredentialsException.class)
    ResponseEntity<ApiResponse<?>> handleBadCredentialsException(
            org.springframework.security.authentication.BadCredentialsException exception) {

        ErrorCode errorCode = ErrorCode.INVALID_CREDENTIALS;

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.error(errorCode));
    }

    //Nếu fail ở Filter authorizeHttpRequests
    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ApiResponse<?>> handleAuthorizationDeniedException(
            AuthorizationDeniedException exception) {

        log.error("AuthorizationDeniedException: ", exception);

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ErrorCode.ACCESS_DENIED));
    }

    //Nếu fail ở @PreAuthorize
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<?>> handleAccessDeniedException(
            AccessDeniedException exception) {

        log.error("AccessDeniedException: ", exception);

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ErrorCode.ACCESS_DENIED));
    }

}
