package hpclab.kcsatspringquestion.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import hpclab.kcsatspringquestion.questionGenerator.dto.ExplanationResponseRawForm;
import hpclab.kcsatspringquestion.questionGenerator.dto.QuestionResponseRawForm;
import hpclab.kcsatspringquestion.questionGenerator.dto.QuestionSubmitKafkaForm;
import hpclab.kcsatspringquestion.questionGenerator.repository.QuestionMemoryRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ExecutionException;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaService {

    private final QuestionMemoryRepository questionMemoryRepository;
    private final QuestionConsumer questionConsumer;
    private final QuestionProducers questionProducers;
    private final ExplanationConsumer explanationConsumer;
    private final ExplanationProducers explanationProducers;
    private final ObjectMapper objectMapper;


    public String getQuestionTopic() {
        return questionProducers.getQuestionTopic();
    }

    public String getExplanationTopic() {
        return explanationProducers.getExplanationTopic();
    }


    // UUID를 사용한 비즈니스 로직 처리
    public Long makeQuestionFromKafka(QuestionSubmitKafkaForm form, HttpSession httpSession) throws InterruptedException, JsonProcessingException, ExecutionException {
        String stringForm = objectMapper.writeValueAsString(form);

        // 메시지 전송
        return questionProducers.sendMessage(stringForm, httpSession);
    }


    // UUID를 사용한 비즈니스 로직 처리
    public QuestionResponseRawForm receiveQuestionFromKafka(HttpSession httpSession) throws InterruptedException {
        // 메시지가 들어올 때까지 대기
        ConsumerRecord<String, String> message = questionConsumer.getMessageFromQueue(httpSession);
        String messageValue = message.value();

        try {
            return objectMapper.readValue(messageValue, QuestionResponseRawForm.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }


    // UUID를 사용한 비즈니스 로직 처리
    public Long makeExplanationFromKafka(QuestionResponseRawForm form, HttpSession httpSession) throws InterruptedException, JsonProcessingException, ExecutionException {

        // 작성된 양식 데이터를 API Json 송신 데이터로 제작
        Map<String, Object> data = new LinkedHashMap<>();
        String explanationDefinition = questionMemoryRepository.getExplanationDefinition(form.getQuestionType());
        data.put("definition", explanationDefinition);
        data.put("title", form.getTitle());
        data.put("mainText", form.getMainText());
        data.put("choices", form.getChoices());
        data.put("answer", form.getAnswer());

        log.info("SEND EX : {}", form);

        // 메시지 전송
        return explanationProducers.sendMessage(objectMapper.writeValueAsString(data), httpSession);
    }


    // UUID를 사용한 비즈니스 로직 처리
    public ExplanationResponseRawForm receiveExplanationFromKafka(HttpSession httpSession) throws InterruptedException {
        // 메시지가 들어올 때까지 대기
        ConsumerRecord<String, String> message = explanationConsumer.getMessageFromQueue(httpSession);
        String messageValue = message.value();

        try {
            return objectMapper.readValue(messageValue, ExplanationResponseRawForm.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }


    // 현재 Consumer의 오프셋 얻기
    public long getRecentConsumedQuestionOffset(HttpSession httpSession) {
        Object offset = httpSession.getAttribute("questionOffset");
        if (offset == null) {
            log.info("now offset is null");
            return 0L;
        }

        log.info("now Question offset : {}", offset);

        String bootstrapServers = "kafka:9092";
        String groupId = "HPCLab";
        String topic = httpSession.getAttribute("questionTopic").toString();
        int partition = 0;

        KafkaOffsetChecker checker = new KafkaOffsetChecker(bootstrapServers);

        long committedOffset = checker.getCommittedOffset(groupId, topic, partition);

        checker.close();


        return committedOffset;
    }


    public long getRecentConsumedExplanationOffset(HttpSession httpSession) {
        Object offset = httpSession.getAttribute("explanationOffset");
        if (offset == null) {
            log.info("now offset is null");
            return 0L;
        }

        log.info("now Explanation offset : {}", offset);

        String bootstrapServers = "kafka:9092";
        String groupId = "HPCLab";
        String topic = httpSession.getAttribute("explanationTopic").toString();
        int partition = 0;

        KafkaOffsetChecker checker = new KafkaOffsetChecker(bootstrapServers);

        long committedOffset = checker.getCommittedOffset(groupId, topic, partition);

        checker.close();


        return committedOffset;
    }
}
