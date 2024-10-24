package hpclab.kcsatspringquestion.questionGenerator.dto;

import hpclab.kcsatspringquestion.questionGenerator.domain.QuestionType;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class QuestionDto {

    private QuestionType questionType;
    private String title;
    private String mainText;
    private List<String> choices;

    private String answer;
    private String translation;
    private String explanation;
}
