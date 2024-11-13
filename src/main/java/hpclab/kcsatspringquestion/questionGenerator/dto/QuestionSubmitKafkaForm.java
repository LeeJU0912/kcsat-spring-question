package hpclab.kcsatspringquestion.questionGenerator.dto;

import lombok.Data;

@Data
public class QuestionSubmitKafkaForm {
    String type;
    String definition;
    String mainText;

    public QuestionSubmitKafkaForm(String type, String definition, String mainText) {
        this.type = type;
        this.definition = definition;
        this.mainText = mainText;
    }
}
