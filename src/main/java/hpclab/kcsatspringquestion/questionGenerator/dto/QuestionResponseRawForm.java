package hpclab.kcsatspringquestion.questionGenerator.dto;

import hpclab.kcsatspringquestion.questionGenerator.domain.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 문제 정보를 담는 DTO 클래스입니다.
 * 해설 정보 생성, 문제 출력을 위해 사용됩니다.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QuestionResponseRawForm {

    /**
     * 문제 유형
     */
    private QuestionType questionType;

    /**
     * 문제 제목
     */
    private String title;

    /**
     * 문제 본문
     */
    private String mainText;

    /**
     * 문제 보기
     */
    private List<String> choices;

    /**
     * 문제 정답
     */
    private String answer;
}