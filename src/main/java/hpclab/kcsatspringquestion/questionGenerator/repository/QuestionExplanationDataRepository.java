package hpclab.kcsatspringquestion.questionGenerator.repository;

import hpclab.kcsatspringquestion.questionGenerator.domain.QuestionExplanation;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 해설 생성 데이터를 DB와 상호작용하는 Spring Data JPA 인터페이스입니다.
 */
public interface QuestionExplanationDataRepository extends JpaRepository<QuestionExplanation, Long> {
}
