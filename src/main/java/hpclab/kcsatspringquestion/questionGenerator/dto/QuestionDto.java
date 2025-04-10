package hpclab.kcsatspringquestion.questionGenerator.dto;

import hpclab.kcsatspringquestion.questionGenerator.domain.QuestionType;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 문제, 해설 정보를 담는 DTO 클래스입니다.
 * 문제, 해설까지 모두 생성 이후 정보를 서빙하는 데에 사용됩니다.
 */
@Data
@Builder
public class QuestionDto {

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

    /**
     * 문제 번역
     */
    private String translation;

    /**
     * 문제 해설
     */
    private String explanation;
}
