package hpclab.kcsatspringquestion.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 모든 에러사항에 대해 중앙 관리하는 열거형 클래스입니다.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    MESSAGE_PROCESSING_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "E101", "데이터 내부 처리에 실패하였습니다."),
    MESSAGE_PARSING_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "E102", "제작된 메시지를 JSON 변환에 실패하였습니다."),
    QUESTION_NOT_READY(HttpStatus.NOT_FOUND, "E103", "아직 문제가 제작되지 않았습니다."),
    EXPLANATION_NOT_READY(HttpStatus.NOT_FOUND, "E104", "아직 해설이 제작되지 않았습니다."),
    DUPLICATE_REQUEST(HttpStatus.UNAUTHORIZED, "E105", "연속해서 요청할 수 없습니다."),
    TOPIC_NOT_FOUND(HttpStatus.NOT_FOUND, "E106", "찾을 수 없는 Kafka Topic입니다."),
    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "E107", "파일을 찾을 수 없습니다."),
    QUESTION_DATA_NOT_FOUND(HttpStatus.NOT_FOUND, "E108", "문제 데이터를 찾을 수 없습니다."),
    EXPLANATION_DATA_NOT_FOUND(HttpStatus.NOT_FOUND, "E109", "해설 데이터를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}