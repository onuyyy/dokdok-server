package com.dokdok.global.exception;

import com.dokdok.global.response.ApiResponse;
import tools.jackson.databind.exc.InvalidFormatException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import com.dokdok.storage.exception.StorageErrorCode;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 공통 예외
     */
    @ExceptionHandler(BaseException.class)
    protected ResponseEntity<ApiResponse<Void>> handleBaseException(BaseException e) {
        BaseErrorCode errorCode = e.getErrorCode();
        log.warn("BaseException: code={}, message={}",
                errorCode.getCode(), e.getMessage());

        return ApiResponse.error(
                errorCode.getStatus(),
                errorCode.getCode(),
                e.getMessage()
        );
    }

    // Authorization Denied Exception 예외처리
    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthorizationDeniedException(
            AuthorizationDeniedException e
    ) {
        log.error("Authorization Denied - Message: {}", e.getMessage());

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ApiResponse<>(
                        GlobalErrorCode.ACCESS_DENIED.getCode(),
                        GlobalErrorCode.ACCESS_DENIED.getMessage(),
                        null
                ));
    }

    // @Valid RequestBody 검증 실패 예외처리
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e
    ) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        log.warn("Validation Failed - Message: {}", errorMessage);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(
                        GlobalErrorCode.INVALID_INPUT_VALUE.getCode(),
                        errorMessage.isEmpty() ? GlobalErrorCode.INVALID_INPUT_VALUE.getMessage() : errorMessage,
                        null
                ));
    }

    // @Validated PathVariable/RequestParam 검증 실패 예외처리
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(
            ConstraintViolationException e
    ) {
        String errorMessage = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));

        log.warn("Constraint Violation - Message: {}", errorMessage);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(
                        GlobalErrorCode.INVALID_INPUT_VALUE.getCode(),
                        errorMessage.isEmpty() ? GlobalErrorCode.INVALID_INPUT_VALUE.getMessage() : errorMessage,
                        null
                ));
    }

    // HTTP Method 불일치 예외처리 (405)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException e
    ) {
        log.warn("Method Not Supported - Method: {}, Message: {}", e.getMethod(), e.getMessage());

        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(new ApiResponse<>(
                        GlobalErrorCode.METHOD_NOT_ALLOWED.getCode(),
                        GlobalErrorCode.METHOD_NOT_ALLOWED.getMessage(),
                        null
                ));
    }

    // 필수 파라미터 누락 예외처리
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException e
    ) {
        String errorMessage = String.format("필수 파라미터 '%s'가 누락되었습니다.", e.getParameterName());
        log.warn("Missing Parameter - Name: {}, Type: {}", e.getParameterName(), e.getParameterType());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(
                        GlobalErrorCode.INVALID_INPUT_VALUE.getCode(),
                        errorMessage,
                        null
                ));
    }

    // 타입 변환 실패 예외처리 (PathVariable, RequestParam)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException e
    ) {
        String errorMessage = String.format("'%s' 값이 올바르지 않습니다.", e.getName());
        log.warn("Type Mismatch - Name: {}, Value: {}, RequiredType: {}",
                e.getName(), e.getValue(), e.getRequiredType());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(
                        GlobalErrorCode.INVALID_TYPE_VALUE.getCode(),
                        errorMessage,
                        null
                ));
    }

    // HTTP Message Not Readable Exception 예외처리 (JSON 파싱 실패)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException e
    ) {
        log.error("Message Not Readable - Message: {}", e.getMessage());

        // enum 파싱 실패인지 확인
        if (e.getCause() instanceof InvalidFormatException ife) {
            if (ife.getTargetType() != null && ife.getTargetType().isEnum()) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(new ApiResponse<>(
                                GlobalErrorCode.INVALID_ENUM_VALUE.getCode(),
                                GlobalErrorCode.INVALID_ENUM_VALUE.getMessage(),
                                null
                        ));
            }
        }

        // 기타 JSON 파싱 오류
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(
                        GlobalErrorCode.INVALID_REQUEST_FORMAT.getCode(),
                        GlobalErrorCode.INVALID_REQUEST_FORMAT.getMessage(),
                        null
                ));
    }

    // 파일 크기 초과 예외처리
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException e
    ) {
        log.warn("Max Upload Size Exceeded - Message: {}", e.getMessage());

        return ResponseEntity
                .status(StorageErrorCode.FILE_SIZE_EXCEEDED.getStatus())
                .body(new ApiResponse<>(
                        StorageErrorCode.FILE_SIZE_EXCEEDED.getCode(),
                        StorageErrorCode.FILE_SIZE_EXCEEDED.getMessage(),
                        null
                ));
    }

    // Runtime Exception 예외처리
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> runtimeExceptionHandler(RuntimeException e) {
        log.error("Runtime Exception - Message: {}", e.getMessage(), e);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(
                        "E000",
                        "서버 에러가 발생했습니다. 담당자에게 문의 바랍니다.",
                        null
                ));
    }

}
