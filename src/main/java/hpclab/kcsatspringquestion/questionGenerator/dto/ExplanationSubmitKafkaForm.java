package hpclab.kcsatspringquestion.questionGenerator.dto;

import hpclab.kcsatspringquestion.questionGenerator.domain.QuestionType;
import lombok.Data;

import java.util.List;


/**
 * 해설 제작을 위해 Kafka에 제출하는 Form DTO입니다.
 * 해설 제작을 위한 지시사항인 explanationDefinition이 추가된 객체입니다.
 */
@Data
public class ExplanationSubmitKafkaForm {
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
     * 해설 생성 Definition
     */
    private String explanationDefinition;

    public ExplanationSubmitKafkaForm(QuestionResponseRawForm form, String explanationDefinition) {
        this.questionType = form.getQuestionType();
        this.title = form.getTitle();
        this.mainText = form.getMainText();
        this.choices = form.getChoices();
        this.answer = form.getAnswer();
        this.explanationDefinition = explanationDefinition;
    }
}
