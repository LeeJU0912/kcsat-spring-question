package hpclab.kcsatspringquestion.questionGenerator.domain;

import hpclab.kcsatspringquestion.questionGenerator.dto.QuestionDto;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;


/**
 * 생성된 문제와 해설을 함께 저장하는 엔티티 클래스입니다.
 */
@Entity(name = "question_explanation_data")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuestionExplanation {

    @Id
    @GeneratedValue
    @Column(name = "question_explanation_id")
    private Long id;

    /**
     * 문제 유형
     */
    @Column(name = "question_explanation_email", nullable = false)
    private QuestionType questionType;

    /**
     * 문제 제목
     */
    @Column(name = "question_explanation_title", nullable = false)
    private String title;

    /**
     * 문제 본문
     */
    @Column(name = "question_explanation_main_text", nullable = false)
    private String mainText;

    /**
     * 문제 보기
     */
    @Setter
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "question_explanation_id")
    private List<Choice> choices;

    /**
     * 문제 번역
     */
    @Column(name = "question_explanation_translation", nullable = false)
    private String translation;

    /**
     * 문제 해설
     */
    @Column(name = "question_explanation_explanation", nullable = false)
    private String explanation;

    /**
     * 문제 정답
     */
    @Column(name = "question_explanation_answer", nullable = false)
    private String answer;

    /**
     * 문제 제작자
     */
    @Column(name = "question_explanation_email", nullable = false)
    private String email;

    public QuestionExplanation(String email, QuestionDto question) {
        this.email = email;
        this.questionType = question.getQuestionType();
        this.title = question.getTitle();
        this.mainText = question.getMainText();
        this.choices = question.getChoices().stream().map(Choice::new).toList();
        this.translation = question.getTranslation();
        this.explanation = question.getExplanation();
        this.answer = question.getAnswer();
    }
}
