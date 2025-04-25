package hpclab.kcsatspringquestion.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import hpclab.kcsatspringquestion.exception.ApiException;
import hpclab.kcsatspringquestion.exception.ErrorCode;
import hpclab.kcsatspringquestion.kafka.comsumer.ExplanationConsumer;
import hpclab.kcsatspringquestion.kafka.comsumer.QuestionConsumer;
import hpclab.kcsatspringquestion.kafka.producer.ExplanationProducer;
import hpclab.kcsatspringquestion.kafka.producer.QuestionProducer;
import hpclab.kcsatspringquestion.questionGenerator.dto.ExplanationResponseRawForm;
import hpclab.kcsatspringquestion.questionGenerator.dto.ExplanationSubmitKafkaForm;
import hpclab.kcsatspringquestion.questionGenerator.dto.QuestionResponseRawForm;
import hpclab.kcsatspringquestion.questionGenerator.dto.QuestionSubmitKafkaForm;
import hpclab.kcsatspringquestion.questionGenerator.repository.QuestionMemoryRepository;
import hpclab.kcsatspringquestion.redis.RedisKeyUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Kafka를 이용한 로직 구현을 담당하는 클래스입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaService {

    /**
     * Kafka GroupId 정의 변수
     */
    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    /**
     * Kafka Partition 정의 변수
     */
    private static final int partition = 0;

    private final QuestionMemoryRepository questionMemoryRepository;

    private final QuestionConsumer questionConsumer;
    private final QuestionProducer questionProducer;

    private final ExplanationConsumer explanationConsumer;
    private final ExplanationProducer explanationProducer;

    private final KafkaOffsetChecker kafkaOffsetChecker;

    private final RedisTemplate<String, String> redisTemplate;

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
     * Question Topic을 발급받은 후, Redis에 1분간 저장합니다. (이후 해당 Topic에 맞는 실시간 Offset을 출력하기 위함)
     * 문제 생성 요청시, 최초 1회 실행되는 메서드입니다.
     *
     * @param email UserEmail
     * @param topic 현재 문제 생성 중인 Topic
     */
    public void setUserQuestionTopic(String email, String topic) {
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(RedisKeyUtil.questionTopicWithEmail(email), topic, Duration.ofMinutes(1));

        if (Boolean.FALSE.equals(locked)) {
            throw new ApiException(ErrorCode.MESSAGE_PROCESSING_ERROR);
        }
    }


    /**
     * 저장된 각 유저별 현재 Question Topic을 가져옵니다.
     * @param email UserEmail
     * @return Question Topic 문자열을 발급받습니다.
     */
    public String getUserQuestionTopic(String email) {
        String topic = redisTemplate.opsForValue().get(RedisKeyUtil.questionTopicWithEmail(email));
        if (topic == null) {
            throw new ApiException(ErrorCode.MESSAGE_PROCESSING_ERROR);
        }

        return topic;
    }


    /**
     * Explanation Topic을 발급받은 후, Redis에 1분간 저장합니다. (이후 해당 Topic에 맞는 실시간 Offset을 출력하기 위함)
     * 해설 생성 요청 시, 최초 1회 실행되는 메서드입니다.
     *
     * @param email UserEmail
     * @param topic 현재 해설 생성 중인 Topic
     */
    public void setUserExplanationTopic(String email, String topic) {
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(RedisKeyUtil.explanationTopicWithEmail(email), topic, Duration.ofSeconds(70));

        if (Boolean.FALSE.equals(locked)) {
            throw new ApiException(ErrorCode.MESSAGE_PROCESSING_ERROR);
        }
    }


    /**
     * 저장된 각 유저별 현재 Explanation Topic을 가져옵니다.
     * @param email UserEmail
     * @return Explanation Topic 문자열을 발급받습니다.
     */
    public String getUserExplanationTopic(String email) {
        String topic = redisTemplate.opsForValue().get(RedisKeyUtil.explanationTopicWithEmail(email));
        if (topic == null) {
            throw new ApiException(ErrorCode.MESSAGE_PROCESSING_ERROR);
        }

        return topic;
    }

    /**
     * 주어진 문제 생성 요청을 Kafka를 통해 전송하는 메서드입니다.
     *
     * <p>사용자의 JWT에서 UserEmail을 추출하여 메시지의 키로 사용하고,
     * {@link QuestionSubmitKafkaForm} 객체를 JSON 문자열로 변환하여 Kafka 토픽에 전송합니다.
     * 메시지를 전송한 후에는 해당 메시지의 Kafka offset 값을 반환합니다.</p>
     *
     * @param form 문제 생성 요청 정보를 담고 있는 DTO 객체
     * @param email 회원 JWT 안의 UserEmail 정보. email 정보는 메시지 키로 활용됩니다.
     * @return Kafka에 메시지를 전송한 후 반환된 메시지의 Offset 값
     */
    public Long makeQuestionFromKafka(QuestionSubmitKafkaForm form, String email) {
        try {
            String topic = getUserQuestionTopic(email);
            return questionProducer.sendMessage(objectMapper.writeValueAsString(form), email, topic);
        } catch (JsonProcessingException e) {
            throw new ApiException(ErrorCode.MESSAGE_PARSING_ERROR);
        }
    }


    /**
     * Kafka로부터 수신된 메시지를 대기하고, {@link QuestionResponseRawForm} 객체로 반환합니다.
     *
     * 꺼낸 메시지의 값을 JSON에서 {@link QuestionResponseRawForm} 객체로 역직렬화합니다.</p>
     *
     * @param email 회원 JWT 안의 userEmail 데이터 (email로 사용자 식별)
     * @return Kafka에서 수신한 메시지를 역직렬화한 {@link QuestionResponseRawForm} 객체.
     */
    public QuestionResponseRawForm receiveQuestionFromKafka(String email) {
        return questionConsumer.getMessage(email);
    }


    /**
     * 문제 해설(Explanation) 요청을 위한 Kafka 메시지를 생성하고 전송합니다.
     *
     * <p>주어진 {@link QuestionResponseRawForm} 데이터를 기반으로 문제 해설 요청 메시지를 구성하고,
     * 이를 Kafka 토픽으로 전송합니다. 메시지는 JWT 안의 UserEmail을 key로 사용하며,
     * 메시지 전송 후 해당 Kafka 메시지의 offset을 반환합니다.</p>
     *
     * @param form 설명 요청에 필요한 문제 정보가 담긴 DTO 객체
     * @param email 회원 JWT 안의 userEmail 데이터 (email로 사용자 식별)
     * @return 전송한 Kafka 메시지의 offset 값
     */
    public Long makeExplanationFromKafka(QuestionResponseRawForm form, String email) {
        String explanationDefinition = questionMemoryRepository.getExplanationDefinition(form.getQuestionType());

        log.info("SEND EX : {}", form);

        try {
            String topic = getUserExplanationTopic(email);

            return explanationProducer.sendMessage(
                    objectMapper.writeValueAsString(
                            new ExplanationSubmitKafkaForm(form, explanationDefinition)), email, topic);
        } catch (JsonProcessingException e) {
            throw new ApiException(ErrorCode.MESSAGE_PARSING_ERROR);
        }
    }


    /**
     * Kafka에서 문제 해설(Explanation) 응답 메시지를 수신하고, 이를 파싱하여 반환합니다.
     *
     * <p>사용자의 JWT 안의 userEmail 데이터를 기반으로 Kafka로부터 수신 대기 중인 메시지를 확인합니다.
     * 큐에 메시지가 존재하지 않으면 ApiException을 반환하며,
     * 존재할 경우 해당 메시지를 {@link ExplanationResponseRawForm} 객체로 역직렬화하여 반환합니다.</p>
     *
     * @param email 회원 JWT 안의 userEmail 데이터 (email로 사용자 식별)
     * @return Kafka로부터 수신한 메시지를 변환한 {@link ExplanationResponseRawForm} 객체
     */
    public ExplanationResponseRawForm receiveExplanationFromKafka(String email) {
        return explanationConsumer.getMessage(email);
    }


    /**
     * 현재 JWT userEmail에 저장된 Kafka Consumer의 오프셋을 조회합니다.
     *
     * <p>세션에서 `questionOffset` 및 `questionTopic` 정보를 가져와 현재 Kafka Consumer가
     * 지정된 Topic과 Partition에서 어디까지 메시지를 소비했는지를 확인합니다.</p>
     *
     * @param email 사용자 JWT userEmail 데이터
     * @return 현재 커밋된 Kafka Consumer 오프셋 값. 값이 없을 경우 0L을 반환합니다.
     */
    public long getRecentConsumedQuestionOffset(String email) {

        String topic = getUserQuestionTopic(email);

        return kafkaOffsetChecker.getCommittedOffset(groupId, topic, partition);
    }


    /**
     * 현재 JWT userEmail에 저장된 Kafka Consumer의 Explanation 관련 오프셋을 조회합니다.
     *
     * <p>세션에서 `explanationOffset` 및 `explanationTopic` 정보를 기반으로,
     * 지정된 Kafka Topic과 Partition에서 현재 커밋된 오프셋 값을 조회합니다.</p>
     *
     * @param email 사용자 JWT userEmail 데이터
     * @return 현재 커밋된 Kafka Consumer 오프셋 값. 값이 없을 경우 0L을 반환합니다.
     */
    public long getRecentConsumedExplanationOffset(String email) {

        String topic = getUserExplanationTopic(email);

        return kafkaOffsetChecker.getCommittedOffset(groupId, topic, partition);
    }
}
