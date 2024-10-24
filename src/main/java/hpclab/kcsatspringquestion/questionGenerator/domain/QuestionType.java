package hpclab.kcsatspringquestion.questionGenerator.domain;

import lombok.Getter;

import java.util.Random;

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
    private static final Random RANDOM = new Random();

    QuestionType(String krName) {
        this.krName = krName;
    }

    public static QuestionType getRandomQuestionType() {
        return QuestionType.values()[RANDOM.nextInt(QuestionType.values().length)];
    }
}