package com.dev.BrandHunt.Common;

import com.dev.BrandHunt.Constant.ErrorCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.validation.ObjectError;
import java.util.List;
import java.util.stream.Collectors;

// ControllerAdvice : 모든 컨트롤러에서 발생하는 예외를 처리하도록 함
@ControllerAdvice
public class GlobalExceptionHandler {
    // CustomException 에서 예외처리를 하면 500 Error로 응답할 수도 있음(응답 포맷이 일정하지 않고, 상태코드 설정 불가능)
    // Exception 어노테이션으로 예외처리를 해당 클래스에서 수행하도록 설정
    // CustomException 을 잡아서 ErrorCode 로부터 상태코드 + 메시지를 꺼내고 JSON 응답으로 반환
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<?> handleCustomException(CustomException e) {
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity
            .status(errorCode.getStatus())
            .body(new ErrorResponse(errorCode.getStatus().value(), errorCode.getMessage()));
    }

    // 예상치 못한 모든 예외 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGeneralException(Exception e) {
        return ResponseEntity
                .status(500)
                .body(new ErrorResponse(500, "알 수 없는 오류가 발생했습니다."));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationException(MethodArgumentNotValidException ex) {

        FieldError fieldError = (FieldError) ex.getBindingResult().getAllErrors().get(0);
        String codeKey = fieldError.getDefaultMessage(); // "EMAIL_REQUIRED", "INVALID_EMAIL" 등

        ErrorCode errorCode;
        try {
            errorCode = ErrorCode.valueOf(codeKey); // Enum 이름과 메시지가 일치할 경우
        } catch (IllegalArgumentException e) {
            errorCode = ErrorCode.INTERNAL_SERVER_ERROR; // fallback
        }

        return new ResponseEntity<>(
                new ErrorResponse(errorCode.getStatus().value(), errorCode.getMessage()),
                errorCode.getStatus()
        );
    }

    // 내부 에러 응답용 DTO
    record ErrorResponse(int status, String message) {}
}
