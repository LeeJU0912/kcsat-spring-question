package hpclab.kcsatspringquestion.questionGenerator.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 문제 안의 보기를 저장한 엔티티 클래스입니다.
 * 하나의 보기 당 하나의 클래스가 할당됩니다.
 */
@Entity(name = "question_choice_data")
@Getter
@NoArgsConstructor
public class Choice {

    /**
     * 보기 ID. DB에서 자동으로 생성됩니다.
     */
    @Id
    @GeneratedValue
    @Column(name = "choice_id")
    private Long id;

    /**
     * 보기 항목
     */
    @Column(nullable = false)
    private String choice;

    public Choice(String choice) {
        this.choice = choice;
    }
}