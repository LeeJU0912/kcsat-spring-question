package hpclab.kcsatspringquestion.questionGenerator.dto;

import lombok.Data;

/**
 * 문제 제작을 위해 Kafka에 제출하는 Form DTO입니다.
 * 문제 제작을 위한 지시사항인 Definition이 추가된 객체입니다.
 */
@Data
public class QuestionSubmitKafkaForm {

    /**
     * 문제 유형
     */
    private String type;

    /**
     * 문제 생성 Definition
     */
    private String definition;

    /**
     * 문제 본문
     */
    private String mainText;

    public QuestionSubmitKafkaForm(String type, String definition, String mainText) {
        this.type = type;
        this.definition = definition;
        this.mainText = mainText;
    }
}
