package hpclab.kcsatspringquestion.kafka.producer;

import hpclab.kcsatspringquestion.exception.ApiException;
import hpclab.kcsatspringquestion.exception.ErrorCode;
import hpclab.kcsatspringquestion.redis.RedisKeyUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * 해설 생성 요청 Kafka producer 클래스입니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExplanationProducer {

    /**
     * GPU 서버 갯수를 나타냅니다.
     */
    private static final Integer EXPLANATION_SERVER_SIZE = 1;

    /**
     * Kafka Topic을 나타냅니다. (이후 추가 가능)
     */
    private static final String EXPLANATION_REQUEST_TOPIC = "ExplanationRequest";

    /**
     * Kafka 메시지를 비동기 전송하기 위한 KafkaTemplate입니다.
     * String 타입의 key-value 메시지를 전송합니다.
     */
    private final KafkaTemplate<String, String> kafkaTemplate;

    /**
     * Kafka 메시지를 저장하기 위한 RedisTemplate입니다.
     * 객체 데이터를 String으로 직렬화하여 저장합니다.
     */
    private final RedisTemplate<String, String> redisTemplate;

    /**
     * Kafka 메시지를 지정된 토픽으로 전송하는 메서드입니다.
     *
     * <p>JWT UserEmail을 키로 사용하여 Kafka 메시지를 발행하며,
     * 메시지를 처리할 토픽은 라운드로빈 방식으로 결정됩니다.</p>
     *
     * 이후 메시지를 보낸 Topic은 Session에 저장한 후, 대기열 확인을 위해 Offset 확인 용도로 사용됩니다.
     *
     * @param message Kafka로 전송할 메시지 문자열
     * @param email 현재 요청의 HttpSession 객체. JWT UserEmail 데이터를 Kafka 메시지의 key로 사용합니다.
     * @param topic 현재 Kafka Topic 값. 해설 생성마다 Topic값이 변경됩니다.
     * @return 전송된 메시지의 Kafka 오프셋 값
     * @throws ApiException 메시지 처리 중 내부 오류가 발생한 경우
     */
    public Long sendMessage(String message, String email, String topic) {

        Boolean locked = redisTemplate.opsForValue().setIfAbsent(RedisKeyUtil.explanationRequestLock(email), "1", Duration.ofMinutes(1));

        if (Boolean.FALSE.equals(locked)) {
            throw new ApiException(ErrorCode.DUPLICATE_REQUEST);
        }

        log.info("sending message to topic: {}, keys: {}", topic, email);
        CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(topic, email, message);

        try {
            return future.get().getRecordMetadata().offset();
        } catch (InterruptedException | ExecutionException e) {
            throw new ApiException(ErrorCode.MESSAGE_PROCESSING_ERROR);
        }
    }

    /**
     * (Topic이 아닌, 파티션 단위로 수정 가능)
     * 해설 생성 Kafka Topic을 불러오는 메서드입니다.
     * 다중 GPU 서버를 사용하는 경우, Topic을 순차적으로 돌아가며 메시지를 Produce 합니다.
     *
     * Topic을 불러오면, Redis에 새롭게 갱신하여 보관합니다.
     *
     * @return Topic에 해당하는 문자열을 반환합니다.
     */
    public String getExplanationTopic() {

        String topic = redisTemplate.opsForValue().get(RedisKeyUtil.explanationTopic());
        if (topic == null) {
            throw new ApiException(ErrorCode.MESSAGE_PROCESSING_ERROR);
        }

        //Round-Robin
        int nextValue = ((Integer.parseInt(topic) + 1) % EXPLANATION_SERVER_SIZE) + 1;
        redisTemplate.opsForValue().set(RedisKeyUtil.explanationTopic(), String.valueOf(nextValue));

        return EXPLANATION_REQUEST_TOPIC + topic;
    }
}
