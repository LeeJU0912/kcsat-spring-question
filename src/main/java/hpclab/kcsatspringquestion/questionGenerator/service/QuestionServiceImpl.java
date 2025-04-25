package hpclab.kcsatspringquestion.questionGenerator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import hpclab.kcsatspringquestion.exception.ApiException;
import hpclab.kcsatspringquestion.exception.ErrorCode;
import hpclab.kcsatspringquestion.questionGenerator.domain.QuestionExplanation;
import hpclab.kcsatspringquestion.questionGenerator.domain.QuestionType;
import hpclab.kcsatspringquestion.questionGenerator.dto.ExplanationResponseRawForm;
import hpclab.kcsatspringquestion.questionGenerator.dto.QuestionDto;
import hpclab.kcsatspringquestion.questionGenerator.dto.QuestionResponseRawForm;
import hpclab.kcsatspringquestion.questionGenerator.repository.QuestionExplanationDataRepository;
import hpclab.kcsatspringquestion.questionGenerator.repository.QuestionMemoryRepository;
import hpclab.kcsatspringquestion.redis.RedisKeyUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 문제 제작 관련 로직을 구현한 클래스입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionServiceImpl implements QuestionService {

    private final QuestionMemoryRepository questionMemoryRepository;
    private final QuestionExplanationDataRepository questionExplanationDataRepository;

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public String getRandomDefaultDataset() {
        return questionMemoryRepository.getDefaultDatasets().get(ThreadLocalRandom.current().nextInt(questionMemoryRepository.getDefaultDatasets().size()));
    }

    @Override
    public String getQuestionDefinition(QuestionType type) {
        return questionMemoryRepository.getQuestionDefinition(type);
    }

    @Override
    public void saveQuestionResult(String email, QuestionResponseRawForm form) {
        try {
            redisTemplate.opsForValue().set(RedisKeyUtil.questionData(email), objectMapper.writeValueAsString(form));
        } catch (JsonProcessingException e) {
            throw new ApiException(ErrorCode.MESSAGE_PROCESSING_ERROR);
        }
    }

    @Override
    public QuestionResponseRawForm getQuestionResult(String email) {
        String data = redisTemplate.opsForValue().get(RedisKeyUtil.questionData(email));
        if (data == null) {
            throw new ApiException(ErrorCode.QUESTION_DATA_NOT_FOUND);
        }

        try {
            return objectMapper.readValue(data, QuestionResponseRawForm.class);
        } catch (JsonProcessingException e) {
            throw new ApiException(ErrorCode.MESSAGE_PROCESSING_ERROR);
        }
    }

    @Override
    public QuestionDto mergeWithExplanation(String email, ExplanationResponseRawForm explanationForm) {

        QuestionResponseRawForm questionForm = getQuestionResult(email);

        QuestionDto question = QuestionDto.builder()
                .title(questionForm.getTitle())
                .questionType(questionForm.getQuestionType())
                .mainText(questionForm.getMainText())
                .choices(questionForm.getChoices().stream().toList())
                .answer(explanationForm.getAnswer())
                .translation(explanationForm.getTranslation())
                .explanation(explanationForm.getExplanation())
                .build();

        saveQuestionExplanationResult(email, question);

        return question;
    }

    private void saveQuestionExplanationResult(String email, QuestionDto questionDto) {
        questionExplanationDataRepository.save(new QuestionExplanation(email, questionDto));
    }
}
