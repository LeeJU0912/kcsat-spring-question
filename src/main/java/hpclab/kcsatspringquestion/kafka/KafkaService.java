package hpclab.kcsatspringquestion.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import hpclab.kcsatspringquestion.kafka.comsumer.ExplanationConsumer;
import hpclab.kcsatspringquestion.kafka.comsumer.QuestionConsumer;
import hpclab.kcsatspringquestion.kafka.producer.ExplanationProducer;
import hpclab.kcsatspringquestion.kafka.producer.QuestionProducer;
import hpclab.kcsatspringquestion.questionGenerator.dto.ExplanationResponseRawForm;
import hpclab.kcsatspringquestion.questionGenerator.dto.QuestionResponseRawForm;
import hpclab.kcsatspringquestion.questionGenerator.dto.QuestionSubmitKafkaForm;
import hpclab.kcsatspringquestion.questionGenerator.repository.QuestionMemoryRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * Kafka를 이용한 로직 구현을 담당하는 클래스입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaService {

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    private final QuestionMemoryRepository questionMemoryRepository;
    private final QuestionConsumer questionConsumer;
    private final QuestionProducer questionProducer;
    private final ExplanationConsumer explanationConsumer;
    private final ExplanationProducer explanationProducer;
    private final KafkaOffsetChecker kafkaOffsetChecker;

    /**
     * Jackson 기반의 JSON 직렬화/역직렬화 객체입니다.
     * 자바 객체를 JSON 문자열로 변환하거나, JSON 문자열을 자바 객체로 변환할 때 사용됩니다.
     */
    private final ObjectMapper objectMapper;


    /**
     * Kafka Question Producer를 위한 Question Topic을 가져옵니다.
     * 여러 클라이언트가 공용으로 다중 GPU 서버 사용을 염두하여 매 요청마다 Topic이 바뀔 수 있습니다.
     *
     * @return Kafka Topic 문자열
     */
    public String getQuestionTopic() {
        return questionProducer.getQuestionTopic();
    }

    /**
     * Kafka Explanation Producer를 위한 Explanation Topic을 가져옵니다.
     * 여러 클라이언트가 공용으로 다중 GPU 서버 사용을 염두하여 매 요청마다 Topic이 바뀔 수 있습니다.
     *
     * @return Kafka Topic 문자열
     */
    public String getExplanationTopic() {
        return explanationProducer.getExplanationTopic();
    }


    /**
     * 주어진 문제 생성 요청을 Kafka를 통해 전송하는 메서드입니다.
     *
     * <p>사용자의 {@link HttpSession}에서 UUID를 추출하여 메시지의 키로 사용하고,
     * {@link QuestionSubmitKafkaForm} 객체를 JSON 문자열로 변환하여 Kafka 토픽에 전송합니다.
     * 메시지를 전송한 후에는 해당 메시지의 Kafka offset 값을 반환합니다.</p>
     *
     * <p>또한, 중복 메시지 처리를 위한 내부 카운트를 증가시켜 이후의 메시지 수신 처리 시 활용됩니다.</p>
     *
     * @param form 문제 생성 요청 정보를 담고 있는 DTO 객체
     * @param httpSession 회원 HTTP 세션 정보. UUID는 메시지 키로 활용됩니다.
     * @return Kafka에 메시지를 전송한 후 반환된 메시지의 Offset 값
     */
    public Long makeQuestionFromKafka(QuestionSubmitKafkaForm form, HttpSession httpSession) {

        questionConsumer.upEraseCount(httpSession);

        try {
            return questionProducer.sendMessage(objectMapper.writeValueAsString(form), httpSession);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("보낼 메시지를 JSON 변환에 실패하였습니다.");
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Kafka로부터 수신된 메시지를 대기하고, {@link QuestionResponseRawForm} 객체로 반환합니다.
     *
     * <p>HTTP 세션의 UUID를 기준으로 메시지 큐에서 가장 최근 메시지를 꺼냅니다.
     * 꺼낸 메시지의 값을 JSON에서 {@link QuestionResponseRawForm} 객체로 역직렬화합니다.</p>
     *
     * @param httpSession 회원 HTTP 세션 객체 (UUID로 사용자 식별)
     * @return Kafka에서 수신한 메시지를 역직렬화한 {@link QuestionResponseRawForm} 객체.
     *         유효한 메시지가 없을 경우 {@code null}을 반환합니다.
     * @throws RuntimeException 메시지를 JSON으로 변환하는 데 실패한 경우 발생합니다.
     */
    public QuestionResponseRawForm receiveQuestionFromKafka(HttpSession httpSession) {
        // 메시지가 들어올 때까지 대기
        ConsumerRecord<String, String> message = questionConsumer.getMessageFromQueue(httpSession);
        if (message == null) {
            return null;
        }

        String messageValue = message.value();

        try {
            return objectMapper.readValue(messageValue, QuestionResponseRawForm.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("받은 메시지를 JSON 변환에 실패하였습니다.");
        }
    }


    /**
     * 문제 해설(Explanation) 요청을 위한 Kafka 메시지를 생성하고 전송합니다.
     *
     * <p>주어진 {@link QuestionResponseRawForm} 데이터를 기반으로 문제 해설 요청 메시지를 구성하고,
     * 이를 Kafka 토픽으로 전송합니다. 메시지는 세션 기반 UUID를 key로 사용하며,
     * 메시지 전송 후 해당 Kafka 메시지의 offset을 반환합니다.</p>
     *
     * @param form 설명 요청에 필요한 문제 정보가 담긴 DTO 객체
     * @param httpSession 회원 HTTP 세션 객체 (UUID 기반 메시지 분기)
     * @return 전송한 Kafka 메시지의 offset 값
     * @throws RuntimeException JSON 직렬화 또는 Kafka 메시지 전송 중 에러가 발생한 경우
     */
    public Long makeExplanationFromKafka(QuestionResponseRawForm form, HttpSession httpSession) {

        // 작성된 양식 데이터를 API Json 송신 데이터로 제작
        Map<String, Object> data = new LinkedHashMap<>();
        String explanationDefinition = questionMemoryRepository.getExplanationDefinition(form.getQuestionType());
        data.put("type", form.getQuestionType().toString());
        data.put("definition", explanationDefinition);
        data.put("title", form.getTitle());
        data.put("mainText", form.getMainText());
        data.put("choices", form.getChoices());
        data.put("answer", form.getAnswer());

        log.info("SEND EX : {}", form);

        explanationConsumer.setEraseCount(httpSession);

        try {
            return explanationProducer.sendMessage(objectMapper.writeValueAsString(data), httpSession);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("보낼 메시지를 JSON 변환에 실패하였습니다.");
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Kafka에서 문제 해설(Explanation) 응답 메시지를 수신하고, 이를 파싱하여 반환합니다.
     *
     * <p>사용자의 HTTP 세션을 기반으로 Kafka로부터 수신 대기 중인 메시지를 확인합니다.
     * 큐에 메시지가 존재하지 않으면 {@code null}을 반환하며,
     * 존재할 경우 해당 메시지를 {@link ExplanationResponseRawForm} 객체로 역직렬화하여 반환합니다.</p>
     *
     * @param httpSession 현재 사용자 HTTP 세션 객체 (UUID 기반으로 메시지를 조회)
     * @return Kafka로부터 수신한 메시지를 변환한 {@link ExplanationResponseRawForm} 객체,
     *         큐에 메시지가 없으면 {@code null}
     * @throws RuntimeException JSON 파싱 실패 시 예외 발생
     */
    public ExplanationResponseRawForm receiveExplanationFromKafka(HttpSession httpSession) {
        // 메시지가 들어올 때까지 대기
        ConsumerRecord<String, String> message = explanationConsumer.checkQueueSizeFromQueue(httpSession);
        if (message == null) {
            return null;
        }

        String messageValue = message.value();

        try {
            return objectMapper.readValue(messageValue, ExplanationResponseRawForm.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * 현재 HTTP 세션에 저장된 Kafka Consumer의 오프셋을 조회합니다.
     *
     * <p>세션에서 `questionOffset` 및 `questionTopic` 정보를 가져와 현재 Kafka Consumer가
     * 지정된 Topic과 Partition에서 어디까지 메시지를 소비했는지를 확인합니다.</p>
     *
     * @param httpSession 사용자 HTTP 세션 객체
     * @return 현재 커밋된 Kafka Consumer 오프셋 값. 값이 없을 경우 0L을 반환합니다.
     */
    public long getRecentConsumedQuestionOffset(HttpSession httpSession) {
        Object offset = httpSession.getAttribute("questionOffset");
        if (offset == null) {
            log.info("now offset is null");
            return 0L;
        }

        log.info("now Question offset : {}", offset);

        String topic = httpSession.getAttribute("questionTopic").toString();
        int partition = 0;

        return kafkaOffsetChecker.getCommittedOffset(groupId, topic, partition);
    }


    /**
     * 현재 HTTP 세션에 저장된 Kafka Consumer의 Explanation 관련 오프셋을 조회합니다.
     *
     * <p>세션에서 `explanationOffset` 및 `explanationTopic` 정보를 기반으로,
     * 지정된 Kafka Topic과 Partition에서 현재 커밋된 오프셋 값을 조회합니다.</p>
     *
     * @param httpSession 사용자 HTTP 세션 객체
     * @return 현재 커밋된 Kafka Consumer 오프셋 값. 값이 없을 경우 0L을 반환합니다.
     */
    public long getRecentConsumedExplanationOffset(HttpSession httpSession) {
        Object offset = httpSession.getAttribute("explanationOffset");
        if (offset == null) {
            log.info("now offset is null");
            return 0L;
        }

        log.info("now Explanation offset : {}", offset);

        String topic = httpSession.getAttribute("explanationTopic").toString();
        int partition = 0;

        return kafkaOffsetChecker.getCommittedOffset(groupId, topic, partition);
    }
}
