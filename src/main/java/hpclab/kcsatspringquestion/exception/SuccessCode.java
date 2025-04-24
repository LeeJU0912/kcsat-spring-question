package hpclab.kcsatspringquestion.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 모든 승인사항에 대해 중앙 관리하는 열거형 클래스입니다.
 */
@Getter
@RequiredArgsConstructor
public enum SuccessCode {
    MESSAGE_SEND_SUCCESS(HttpStatus.OK, "S101", "생성 요청이 정상적으로 처리되었습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}