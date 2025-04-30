package com.dev.BrandHunt.Common;

import lombok.Getter;

@Getter
public class CustomException extends RuntimeException {
    // 실행 시 발생하는 예외 처리를 구현할 경우 RuntimeException 상속받아 구현한다.
    private final ErrorCode errorCode;

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

}
