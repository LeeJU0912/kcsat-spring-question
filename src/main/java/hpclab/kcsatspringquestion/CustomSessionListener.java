package hpclab.kcsatspringquestion;

import hpclab.kcsatspringquestion.kafka.comsumer.ExplanationConsumer;
import hpclab.kcsatspringquestion.kafka.comsumer.QuestionConsumer;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Spring 서버 Session을 감지하고 처리하는 클래스입니다.
 */
@Component
@RequiredArgsConstructor
public class CustomSessionListener implements HttpSessionListener {

    private final QuestionConsumer questionConsumer;
    private final ExplanationConsumer explanationConsumer;

    /**
     * 세션 종료 시 호출되는 콜백 메서드입니다.
     *
     * <p>사용자 세션이 만료되거나 수동으로 제거될 때 실행되며,
     * Kafka 메시지 큐에서 해당 세션에 대한 BlockingQueue를 제거합니다.</p>
     *
     * @param se 세션 종료 이벤트 객체
     */
    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        HttpSession session = se.getSession();

        questionConsumer.deleteSessionFromMessageQueue(session.getId());
        explanationConsumer.deleteSessionFromMessageQueue(session.getId());
    }
}
