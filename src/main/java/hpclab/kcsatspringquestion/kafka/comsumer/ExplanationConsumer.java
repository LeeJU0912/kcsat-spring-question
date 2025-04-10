package hpclab.kcsatspringquestion.kafka.comsumer;


import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Kafka 해설 생성 결과 Comsumer 클래스입니다.
 * AI 서버에서 문제에 대한 해설을 생성해서 produce하면, 이 클래스에서 comsume합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExplanationConsumer {

    private static final String EXPLANATION_RESPONSE_TOPIC = "ExplanationResponse";

    /**
     * 같은 클라이언트로부터 단시간에 많은 요청이 들어오는 경우, 가장 최근 요청만을 가져오기 위해 카운터를 셉니다.
     * 쌓여있는 값이 1 이상인 경우(=이전에 요청하고 받지 않은 값이 많은 경우), 가장 나중에 들어온 메시지 하나를 제외한 나머지를 전부 버립니다.
     */
    private static final Map<String, Integer> eraseCount = new ConcurrentHashMap<>();


    /**
     * 사용자별 메시지 큐를 저장하는 맵입니다.
     *
     * &lt;UUID, BlockingQueue&gt; 형태로 구성되어 있으며,
     * UUID는 각 사용자 또는 세션을 식별하는 키로 사용되고,
     * BlockingQueue는 해당 사용자의 Kafka 메시지를 임시 저장하는 큐입니다.
     */
    private static final Map<String, BlockingQueue<ConsumerRecord<String, String>>> messageQueue = new ConcurrentHashMap<>();


    /**
     * Kafka에서 {@code EXPLANATION_RESPONSE_TOPIC} 토픽으로부터 메시지를 수신하는 메서드입니다.
     * 수신된 메시지는 사용자별 메시지 큐에 저장되어 비동기 응답 처리에 사용됩니다.
     *
     * <p>메시지의 key는 사용자 또는 세션 식별자 역할을 하며,
     * 해당 key에 매핑되는 {@link BlockingQueue}가 존재하지 않으면 새로 생성됩니다.
     * 이후 메시지는 해당 큐에 저장되어 다른 스레드 또는 요청 처리 로직에서 소비됩니다.</p>
     *
     * @param record Kafka에서 수신한 메시지. key는 사용자 식별자, value는 메시지 본문입니다.
     * @throws InterruptedException 메시지를 큐에 저장하는 과정에서 인터럽트가 발생한 경우
     */
    @KafkaListener(topics = EXPLANATION_RESPONSE_TOPIC)
    public void listen(ConsumerRecord<String, String> record) throws InterruptedException {
        log.info("Received Consumer Record : {}", record);

        // 처음 접속인 경우 messageQueue를 새롭게 생성한다.
        createSessionInMessageQueue(record.key());

        messageQueue.get(record.key()).put(record);
    }

    /**
     * HttpSession 객체에서 UUID를 추출하여 Queue를 개인화하는 메서드입니다.
     *
     * @param httpSession HTTP 세션 객체
     */
    public void setEraseCount(HttpSession httpSession) {
        String uuid = httpSession.getId();

        log.info("Get UUID : {}", uuid);

        // 처음 접속인 경우 messageQueue를 새롭게 생성한다.
        createSessionInMessageQueue(uuid);

        eraseCount.put(uuid, eraseCount.get(uuid) + 1);
    }

    /**
     * Queue에서 메시지를 가져옵니다. 만약 아직 생성중이라 Queue가 비어 있다면 대기합니다.
     *
     * @param httpSession HTTP 세션 객체
     * @return 이전에 들어온 메시지는 전부 버리고, 가장 최근 메시지만을 consume하여 반환합니다.
     */
    public ConsumerRecord<String, String> checkQueueSizeFromQueue(HttpSession httpSession) {

        String uuid = httpSession.getId();

        log.info("Get UUID : {}", uuid);

        BlockingQueue<ConsumerRecord<String, String>> nowQueue = messageQueue.get(uuid);

        return getFreshMessage(nowQueue, uuid);
    }

    /**
     * 가장 최근 메시지만 가져오는 메서드입니다.
     *
     * @param nowQueue 현재 누적되는 BlockingQueue
     * @param uuid 회원 세션 UUID
     * @return 가장 최근에 요청한 메시지만 consume하여 반환합니다.
     */
    private ConsumerRecord<String, String> getFreshMessage(BlockingQueue<ConsumerRecord<String, String>> nowQueue, String uuid) {
        if (nowQueue.size() == eraseCount.get(uuid)) {
            while(eraseCount.get(uuid) > 1) {
                nowQueue.poll();
                eraseCount.put(uuid, eraseCount.get(uuid) - 1);
            }

            eraseCount.put(uuid, eraseCount.get(uuid) - 1);
            return nowQueue.poll();
        } else {
            return null;
        }
    }

    /**
     * 만약 최초 접속이라면, 해당 UUID에 대응하는 BlockingQueue를 할당하는 메서드입니다.
     *
     * @param uuid 유저 세션 UUID
     */
    private void createSessionInMessageQueue(String uuid) {
        if (!messageQueue.containsKey(uuid)) {
            messageQueue.put(uuid, new LinkedBlockingQueue<>());
            eraseCount.put(uuid, 0);
        }
    }

    /**
     * 세션이 삭제될 때, 남아 있는 Queue를 삭제하는 메서드입니다.
     *
     * @param uuid 유저 세션 UUID
     */
    public void deleteSessionFromMessageQueue(String uuid) {
        messageQueue.remove(uuid);
        eraseCount.remove(uuid);
    }
}
