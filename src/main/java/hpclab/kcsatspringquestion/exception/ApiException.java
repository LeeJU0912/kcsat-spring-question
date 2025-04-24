package hpclab.kcsatspringquestion.exception;

import lombok.Getter;

/**
 * 예외 상황에 대해 직접 관리하기 위한 커스텀 예외사항입니다. 이를 활용하여 일관된 에러 형식으로 반환이 가능합니다.
 */
@Getter
public class ApiException extends RuntimeException {
    private final ErrorCode errorCode;

    public ApiException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}