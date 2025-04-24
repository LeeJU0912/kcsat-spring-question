package hpclab.kcsatspringquestion.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 사용자에게 반환하는 모든 방식에 대해 구조화하기 위한 클래스입니다.
 * success: 응답 성공/실패
 * data(옵션): 만약 데이터가 있는 경우 실어서 보냅니다.(없는 경우에는 null 반환)
 * code(옵션): 만약 특별한 응답 코드가 존재하는 경우 실어서 보냅니다.(없는 경우에는 null 반환)
 * message(옵션): 만약 특별한 응답 메시지가 존재하는 경우 실어서 보냅니다.(없는 경우에는 null 반환)
 *
 * @param <T> 모든 타입의 객체 데이터를 전송하기 위해 제네릭을 사용합니다.
 */
@Getter
@AllArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private T data;
    private String code;
    private String message;
}