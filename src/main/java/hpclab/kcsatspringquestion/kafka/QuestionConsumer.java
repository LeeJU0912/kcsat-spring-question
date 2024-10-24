package hpclab.kcsatspringquestion.kafka;


import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
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

@Component
@RequiredArgsConstructor
public class QuestionConsumer {

    private final Logger logger = LoggerFactory.getLogger(KafkaConsumer.class);

    // BlockingQueue를 사용하여 메시지를 저장
    private final Map<String, BlockingQueue<ConsumerRecord<String, String>>> messageQueue = new ConcurrentHashMap<>();

    @KafkaListener(topics = "QuestionResponse")
    public void listen(ConsumerRecord<String, String> record) throws InterruptedException {
        logger.info("Received Question record : {}", record);

        // 처음 접속인 경우 messageQueue를 새롭게 생성한다.
        createSessionInMessageQueue(record.key());

        messageQueue.get(record.key()).put(record);
    }


    // Queue에서 메시지 가져오기 (큐가 비어 있으면 대기)
    public ConsumerRecord<String, String> getMessageFromQueue(HttpSession httpSession) throws InterruptedException {

        String uuid = httpSession.getId();

        logger.info("Get UUID : {}", uuid);

        // 처음 접속인 경우 messageQueue를 새롭게 생성한다.
        createSessionInMessageQueue(uuid);

        // 페이지 벗어났을 때 이미 생성한 것은 무효로 한다.
        if (!messageQueue.get(uuid).isEmpty()) {
            messageQueue.get(uuid).clear();
        }

        // 메시지가 들어올 때까지 대기
        return messageQueue.get(uuid).take();
    }


    private void createSessionInMessageQueue(String uuid) {
        if (!messageQueue.containsKey(uuid)) {
            messageQueue.put(uuid, new LinkedBlockingQueue<>());
        }
    }

    public void deleteSessionFromMessageQueue(String uuid) {
        messageQueue.remove(uuid);
    }
}
