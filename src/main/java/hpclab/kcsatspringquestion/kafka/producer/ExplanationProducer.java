package hpclab.kcsatspringquestion.kafka.producer;

import hpclab.kcsatspringquestion.exception.ApiException;
import hpclab.kcsatspringquestion.exception.ErrorCode;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 해설 생성 요청 Kafka producer 클래스입니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExplanationProducer {

    /**
     * 모든 클라이언트가 순서를 공유하는 것으로 동시성 보장을 신경써야 합니다.
     * AtomicInteger로 반드시 순차적으로 값 수정을 할 수 있도록 하였습니다.
     */
    private static final AtomicInteger produceIdx = new AtomicInteger(0);

    /**
     * GPU 서버 갯수를 나타냅니다.
     */
    private static final Integer EXPLANATION_SERVER_SIZE = 1;

    /**
     * Kafka Topic을 나타냅니다. (이후 추가 가능)
     */
    private static final String EXPLANATION_REQUEST_TOPIC_1 = "ExplanationRequest1";

    /**
     * Kafka 메시지를 비동기 전송하기 위한 KafkaTemplate입니다.
     * String 타입의 key-value 메시지를 전송합니다.
     */
    private final KafkaTemplate<String, String> kafkaTemplate;

    /**
     * Kafka 메시지를 지정된 토픽으로 전송하는 메서드입니다.
     *
     * <p>세션의 UUID를 키로 사용하여 Kafka 메시지를 발행하며,
     * 메시지를 처리할 토픽은 라운드로빈 방식으로 결정됩니다.</p>
     *
     * 이후 메시지를 보낸 Topic은 Session에 저장한 후, 대기열 확인을 위해 Offset 확인 용도로 사용됩니다.
     *
     * @param message Kafka로 전송할 메시지 문자열
     * @param httpSession 현재 요청의 HttpSession 객체. 세션 ID를 Kafka 메시지의 key로 사용합니다.
     * @return 전송된 메시지의 Kafka 오프셋 값
     * @throws ApiException 메시지 처리 중 내부 오류가 발생한 경우
     */
    public Long sendMessage(String message, HttpSession httpSession) {

        String uuid = httpSession.getId();
        String topic = httpSession.getAttribute("explanationTopic").toString();

        log.info("sending message to topic: {}, keys: {}", topic, uuid);
        CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(topic, uuid, message);

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
     * 불러온 Topic은 세션 정보에 보관합니다.
     *
     * @return Topic에 해당하는 문자열을 반환합니다.
     */
    public String getExplanationTopic() {
        // Round-Robin
        int index = produceIdx.updateAndGet(i -> (i + 1) % EXPLANATION_SERVER_SIZE);

        if (index == 0) {
            return EXPLANATION_REQUEST_TOPIC_1;
        } else if (index == 1) { // 다른 Topic 추가 가능
            return EXPLANATION_REQUEST_TOPIC_1;
        } else {
            throw new ApiException(ErrorCode.TOPIC_NOT_FOUND);
        }
    }
}
