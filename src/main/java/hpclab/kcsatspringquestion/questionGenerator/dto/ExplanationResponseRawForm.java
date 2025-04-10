package hpclab.kcsatspringquestion.questionGenerator.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 해설 정보를 담는 DTO 클래스입니다.
 * 해설 제작 이후, 이곳에 담겨서 서빙됩니다.
 */
@Data
@Builder
public class ExplanationResponseRawForm {

    /**
     * 문제 번역
     */
    private String translation;

    /**
     * 문제 해설
     */
    private String explanation;

    /**
     * 문제 정답
     */
    private String answer;
}
