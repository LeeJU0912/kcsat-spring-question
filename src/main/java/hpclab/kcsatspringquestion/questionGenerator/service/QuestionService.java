package hpclab.kcsatspringquestion.questionGenerator.service;

import hpclab.kcsatspringquestion.questionGenerator.domain.QuestionType;

public interface QuestionService {
    String getRandomDefaultDataset();
    String getQuestionDefinition(QuestionType type);
}
