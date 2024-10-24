package hpclab.kcsatspringquestion.questionGenerator.dto;

import hpclab.kcsatspringquestion.questionGenerator.domain.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QuestionResponseRawForm {

    private QuestionType questionType;

    private String title;
    private String mainText;
    private List<String> choices;

    private String answer;
}