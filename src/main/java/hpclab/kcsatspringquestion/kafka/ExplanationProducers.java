package hpclab.kcsatspringquestion.kafka;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Component
@RequiredArgsConstructor
public class ExplanationProducers {

    private static final Integer EXPLANATION_SERVER_SIZE = 1;
    private static final String EXPLANATION_REQUEST_TOPIC_1 = "ExplanationRequest2";

    private final Logger logger = LoggerFactory.getLogger(KafkaProducer.class);
    private final KafkaTemplate<String, String> kafkaTemplate;
    private int produceIdx = 1;

    public Long sendMessage(String message, HttpSession httpSession) throws ExecutionException, InterruptedException {

        String uuid = httpSession.getId();
        String topic = httpSession.getAttribute("explanationTopic").toString();

        logger.info("sending message to topic: {}, keys: {}", topic, uuid);
        CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(topic, uuid, message);

        return future.get().getRecordMetadata().offset();
    }

    public String getExplanationTopic() {
        //RoundRobin
        produceIdx = (produceIdx + 1) % EXPLANATION_SERVER_SIZE;

        if (produceIdx == 0) {
            return EXPLANATION_REQUEST_TOPIC_1;
        } else if (produceIdx == 1) {
            return EXPLANATION_REQUEST_TOPIC_1;
        } else {
            throw new IllegalArgumentException("Invalid explanation topic");
        }
    }
}
