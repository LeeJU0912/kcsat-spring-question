package hpclab.kcsatspringquestion.questionGenerator.service;

import hpclab.kcsatspringquestion.questionGenerator.domain.QuestionType;

/**
 * 문제 생성 관련 로직을 정의한 인터페이스입니다.
 */
public interface QuestionService {

    /**
     * 기출 지문들 중 무작위로 하나를 뽑아 반환하는 메서드입니다.
     *
     * @return 기출 지문이 하나 반환됩니다.
     */
    String getRandomDefaultDataset();

    /**
     * 문제 유형에 맞는 Definition을 찾아 반환하는 메서드입니다.
     *
     * @param type 문제 유형
     * @return Definition
     */
    String getQuestionDefinition(QuestionType type);
}
