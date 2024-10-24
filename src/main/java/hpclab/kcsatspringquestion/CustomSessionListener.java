package hpclab.kcsatspringquestion;

import hpclab.kcsatspringquestion.kafka.ExplanationConsumer;
import hpclab.kcsatspringquestion.kafka.QuestionConsumer;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomSessionListener implements HttpSessionListener {

    private final QuestionConsumer questionConsumer;
    private final ExplanationConsumer explanationConsumer;

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        HttpSession session = se.getSession();

        questionConsumer.deleteSessionFromMessageQueue(session.getId());
        explanationConsumer.deleteSessionFromMessageQueue(session.getId());
    }
}
