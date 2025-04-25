package hpclab.kcsatspringquestion.kafka.comsumer;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import hpclab.kcsatspringquestion.exception.ApiException;
import hpclab.kcsatspringquestion.exception.ErrorCode;
import hpclab.kcsatspringquestion.questionGenerator.dto.ExplanationResponseRawForm;
import hpclab.kcsatspringquestion.redis.RedisKeyUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;

/**
 * Kafka 해설 생성 결과 Comsumer 클래스입니다.
 * AI 서버에서 문제에 대한 해설을 생성해서 produce하면, 이 클래스에서 comsume합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExplanationConsumer {

    private static final String EXPLANATION_RESPONSE_TOPIC = "ExplanationResponse";

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Kafka에서 {@code EXPLANATION_RESPONSE_TOPIC} 토픽으로부터 메시지를 수신하는 메서드입니다.
     * 수신된 메시지는 사용자별 메시지 큐에 저장되어 비동기 응답 처리에 사용됩니다.
     *
     * <p>메시지의 key는 사용자 또는 세션 식별자 역할을 하며,
     * 해당 key에 매핑되는 {@link BlockingQueue}가 존재하지 않으면 새로 생성됩니다.
     * 이후 메시지는 해당 큐에 저장되어 다른 스레드 또는 요청 처리 로직에서 소비됩니다.</p>
     *
     * @param record Kafka에서 수신한 메시지. key는 사용자 식별자, value는 메시지 본문입니다.
     * @throws ApiException 메시지를 파싱하는 데 오류가 발생하거나, 중복 요청이 들어오는 경우 발생합니다.
     */
    @KafkaListener(topics = EXPLANATION_RESPONSE_TOPIC)
    public void listen(ConsumerRecord<String, String> record) {
        log.info("Received Consumer Record : {}", record.key());

        try {
            ExplanationResponseRawForm response = objectMapper.readValue(record.value(), ExplanationResponseRawForm.class);

            String email = record.key();

            redisTemplate.opsForValue().set(RedisKeyUtil.explanationMessage(email), objectMapper.writeValueAsString(response));

        } catch (JsonProcessingException e) {
            throw new ApiException(ErrorCode.MESSAGE_PROCESSING_ERROR);
        }
    }

    /**
     * 만들어진 메시지를 가져오는 메서드입니다.
     *
     * @param email 요청 UserEmail
     * @return 만들어진 메시지를 consume하여 반환합니다.
     */
    public ExplanationResponseRawForm getMessage(String email) {
        // 메시지가 들어올 때까지 대기
        if (!redisTemplate.hasKey(RedisKeyUtil.explanationMessage(email))) {
            throw new ApiException(ErrorCode.EXPLANATION_NOT_READY);
        }

        try {
            String value = redisTemplate.opsForValue().get(RedisKeyUtil.explanationMessage(email));
            redisTemplate.delete(RedisKeyUtil.explanationMessage(email));
            return objectMapper.readValue(value, ExplanationResponseRawForm.class);
        } catch (JsonProcessingException e) {
            throw new ApiException(ErrorCode.MESSAGE_PROCESSING_ERROR);
        }
    }
}
