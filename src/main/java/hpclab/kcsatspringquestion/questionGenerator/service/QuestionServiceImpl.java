package hpclab.kcsatspringquestion.questionGenerator.service;

import hpclab.kcsatspringquestion.questionGenerator.domain.QuestionType;
import hpclab.kcsatspringquestion.questionGenerator.repository.QuestionMemoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 문제 제작 관련 로직을 구현한 클래스입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionServiceImpl implements QuestionService {

    private final QuestionMemoryRepository questionMemoryRepository;

    @Override
    public String getRandomDefaultDataset() {
        return questionMemoryRepository.getDefaultDatasets().get(ThreadLocalRandom.current().nextInt(questionMemoryRepository.getDefaultDatasets().size()));
    }

    @Override
    public String getQuestionDefinition(QuestionType type) {
        return questionMemoryRepository.getQuestionDefinition(type);
    }
}
