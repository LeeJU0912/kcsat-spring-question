package hpclab.kcsatspringquestion.questionGenerator.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 문제 제작을 위한 제출 Form DTO 클래스입니다.
 * 문제 최초 제작 시 사용자가 작성하는 용도로 사용됩니다.
 */
@Data
@AllArgsConstructor
public class QuestionSubmitRawForm {

    /**
     * 문제 유형
     */
    private String type;

    /**
     * 문제 본문
     */
    private String mainText;
}
