package hpclab.kcsatspringquestion.questionGenerator.domain;

import lombok.Getter;

import java.util.Random;

/**
 * 문제 유형을 정리해 놓은 열거형 클래스입니다.
 * 총 15가지 유형을 서비스하고 있습니다.
 */
@Getter
public enum QuestionType {

    PURPOSE("글의 목적"),
    MAIN_IDEA("글의 요지"),
    TITLE("글의 제목 추론"),
    TOPIC("글의 주제"),
    TARGET_UNMATCH("대상 정보 불일치"),

    FEELING_CHANGE("화자 심경 변화"),
    UNDERLINE("밑줄 친 구문 의미"),
    BLANK("빈칸 추론"),
    BLANK_AB("빈칸 A,B 추론"),
    INFO_MATCH("안내문 일치"),

    INFO_UNMATCH("안내문 불일치"),
    GRAMMAR("어법"),
    SUMMARIZE_AB("요약문 A,B 추론"),
    ORDERING("이어질 글의 순서 배열"),
    ARGUMENT("필자가 주장하는 바");

    private final String krName;
    QuestionType(String krName) {
        this.krName = krName;
    }

    private static final Random RANDOM = new Random();

    /**
     * 문제 유형을 무작위로 추출하는 메서드입니다.
     * 무작위 문제 제작에 사용됩니다.
     *
     * @return 랜덤하게 뽑힌 QuestionType
     */
    public static QuestionType getRandomQuestionType() {
        return QuestionType.values()[RANDOM.nextInt(QuestionType.values().length)];
    }
}