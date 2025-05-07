package com.dev.BrandHunt.Constant;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    // 400(잘못된 요청) 요청한 구문 오류
    EMAIL_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "이미 사용 중인 이메일입니다."),
    NICKNAME_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "이미 사용 중인 닉네임입니다."),
    MISSING_REQUIRED_FIELDS(HttpStatus.BAD_REQUEST, "필수 입력 항목이 누락되었습니다."),
    EMPTY_SEARCH_QUERY(HttpStatus.BAD_REQUEST, "검색어를 입력하세요."),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "잘못된 입력입니다."),
    FIELD_TOO_SHORT(HttpStatus.BAD_REQUEST, "입력값이 너무 짧습니다."),
    FIELD_TOO_LONG(HttpStatus.BAD_REQUEST, "입력값이 너무 깁니다."),
    DUPLICATE_ENTRY(HttpStatus.BAD_REQUEST, "이미 존재하는 항목입니다."),
    INVALID_DATE_FORMAT(HttpStatus.BAD_REQUEST, "잘못된 날짜 형식입니다."),
    INVALID_PHONE_NUMBER(HttpStatus.BAD_REQUEST, "잘못된 전화번호 형식입니다."),
    INVALID_VERIFY_CODE(HttpStatus.BAD_REQUEST, "인증 코드가 일치하지 않습니다."),
    EMAIL_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "이메일 인증이 완료되지 않았습니다."),

    // 401(권한 없음) 지정한 리소스에 대한 권한없음
    USER_INACTIVE(HttpStatus.UNAUTHORIZED, "비활성화된 사용자입니다. 관리자에게 문의하세요."),
    USER_SUSPENDED(HttpStatus.FORBIDDEN, "정지된 사용자입니다. 관리자에게 문의하세요."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "비밀번호가 일치하지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),

    // 403(금지됨) 지정한 리소스에 대한 액세스 금지
    FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다."),

    // 404(찾을 수 없음) 지정한 리소스를 찾을 수 없음
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    VERIFY_NOT_FOUND(HttpStatus.NOT_FOUND, "인증 코드가 만료되었거나 존재하지 않습니다."),

    // 409(리소스 충돌)
    AUTH_EMAIL_DUPLICATED(HttpStatus.CONFLICT, "이미 인증 메일이 발송되었습니다. 이메일을 확인하세요."),

    // 500(내부 서버 오류) 서버에 에러가 발생
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

}
