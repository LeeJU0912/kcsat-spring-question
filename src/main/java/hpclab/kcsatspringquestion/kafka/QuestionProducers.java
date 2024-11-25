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
public class QuestionProducers {

    private static final Integer QUESTION_SERVER_SIZE = 1;
    private static final String QUESTION_REQUEST_TOPIC_1 = "QuestionRequest2";

    private final Logger logger = LoggerFactory.getLogger(KafkaProducer.class);
    private final KafkaTemplate<String, String> kafkaTemplate;
    private int produceIdx = 1;

    public Long sendMessage(String message, HttpSession httpSession) throws ExecutionException, InterruptedException {

        String uuid = httpSession.getId();
        String topic = httpSession.getAttribute("questionTopic").toString();

        logger.info("sending message to topic: {}, keys: {}", topic, uuid);
        CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(topic, uuid, message);
        return future.get().getRecordMetadata().offset();
    }

    public String getQuestionTopic() {
        //RoundRobin
        produceIdx = (produceIdx + 1) % QUESTION_SERVER_SIZE;

        if (produceIdx == 0) {
            return QUESTION_REQUEST_TOPIC_1;
        } else if (produceIdx == 1) {
            return QUESTION_REQUEST_TOPIC_1;
        } else {
            throw new IllegalArgumentException("Invalid question topic");
        }
    }
}
