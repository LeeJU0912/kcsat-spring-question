package hpclab.kcsatspringquestion.questionGenerator.service;

import hpclab.kcsatspringquestion.questionGenerator.domain.QuestionType;
import hpclab.kcsatspringquestion.questionGenerator.dto.ExplanationResponseRawForm;
import hpclab.kcsatspringquestion.questionGenerator.dto.QuestionDto;
import hpclab.kcsatspringquestion.questionGenerator.dto.QuestionResponseRawForm;

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

    /**
     * 문제 결과 데이터를 DB에 저장하는 메서드입니다.
     *
     * @param email 문제 제작을 요청한 회원 email
     * @param form 제작된 문제 Data DTO
     */
    void saveQuestionResult(String email, QuestionResponseRawForm form);

    /**
     * 문제 결과 데이터를 DB에서 찾아 반환하는 메서드입니다.
     *
     * @param email 문제 정보를 요청한 회원 email
     * @return 회원이 생성한 문제 Data DTO
     */
    QuestionResponseRawForm getQuestionResult(String email);

    /**
     * 생성한 문제 데이터를 DB에서 찾고, 생성한 해설 데이터를 합쳐 하나의 객체로 반환하는 메서드입니다.
     * @param email 해설 생성을 요청한 회원 email
     * @param form 생성된 해설 Data DTO
     * @return 문제와 해설 정보가 모두 들어 있는 Data DTO
     */
    QuestionDto mergeWithExplanation(String email, ExplanationResponseRawForm form);
}
