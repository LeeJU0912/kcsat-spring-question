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

    private final Map<String, Integer> eraseCount = new ConcurrentHashMap<>();

    @KafkaListener(topics = "QuestionResponse")
    public void listen(ConsumerRecord<String, String> record) throws InterruptedException {
        logger.info("Received Question record : {}", record);

        // 처음 접속인 경우 messageQueue를 새롭게 생성한다.
        createSessionInMessageQueue(record.key());

        messageQueue.get(record.key()).put(record);
    }

    public void setEraseCount(HttpSession httpSession) {
        String uuid = httpSession.getId();

        logger.info("Get UUID : {}", uuid);

        // 처음 접속인 경우 messageQueue를 새롭게 생성한다.
        createSessionInMessageQueue(uuid);

        eraseCount.put(uuid, eraseCount.get(uuid) + 1);
    }

    // Queue에서 메시지 가져오기 (큐가 비어 있으면 대기)
    public ConsumerRecord<String, String> checkQueueSizeFromQueue(HttpSession httpSession) {

        String uuid = httpSession.getId();

        logger.info("Get UUID : {}", uuid);

        BlockingQueue<ConsumerRecord<String, String>> nowQueue = messageQueue.get(uuid);


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

    private void createSessionInMessageQueue(String uuid) {
        if (!messageQueue.containsKey(uuid)) {
            messageQueue.put(uuid, new LinkedBlockingQueue<>());
            eraseCount.put(uuid, 0);
        }
    }

    public void deleteSessionFromMessageQueue(String uuid) {
        messageQueue.remove(uuid);
        eraseCount.remove(uuid);
    }
}
