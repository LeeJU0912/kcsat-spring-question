package hpclab.kcsatspringquestion.questionGenerator.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import hpclab.kcsatspringquestion.questionGenerator.domain.QuestionType;
import hpclab.kcsatspringquestion.kafka.KafkaService;
import hpclab.kcsatspringquestion.questionGenerator.dto.*;
import hpclab.kcsatspringquestion.questionGenerator.service.QuestionService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.ExecutionException;

@Slf4j
@RestController
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;
    private final KafkaService kafkaService;


    // 최초 Session UUID 생성
    @GetMapping("/api/question/firstQuestionContact")
    public ResponseEntity<String> createQuestionData(HttpSession httpSession) {
        log.info("First get Session ID: {}", httpSession.getId());

        // topic 설정
        String questionTopic = kafkaService.getQuestionTopic();

        // session에 topic 저장
        httpSession.setAttribute("questionTopic", questionTopic);

        return ResponseEntity.ok(httpSession.getId());
    }

    // 최초 Session UUID 생성
    @GetMapping("/api/question/firstExplanationContact")
    public ResponseEntity<String> createExplanationData(HttpSession httpSession) {
        log.info("First get Session ID: {}", httpSession.getId());

        // topic 설정
        String explanationTopic = kafkaService.getExplanationTopic();

        // session에 topic 저장
        httpSession.setAttribute("explanationTopic", explanationTopic);

        return ResponseEntity.ok(httpSession.getId());
    }


    // 현재 offset 찾기
    @GetMapping("/api/question/questionOffsetGap")
    public ResponseEntity<Long> getQuestionOffset(HttpSession httpSession) {
        long recentSentQuestionOffset = (long) httpSession.getAttribute("questionOffset");
        long recentConsumedQuestionOffset = kafkaService.getRecentConsumedQuestionOffset(httpSession);

        long calculatedOffset = recentSentQuestionOffset - recentConsumedQuestionOffset + 1;

        log.info("Session ID: {}", httpSession.getId());
        log.info("Calculated Offset: {}", calculatedOffset);

        return ResponseEntity.ok(calculatedOffset);
    }


    // 현재 offset 찾기
    @GetMapping("/api/question/explanationOffsetGap")
    public ResponseEntity<Long> getExplanationOffset(HttpSession httpSession) {
        long recentSentExplanationOffset = (long) httpSession.getAttribute("explanationOffset");
        long recentConsumedExplanationOffset = kafkaService.getRecentConsumedExplanationOffset(httpSession);

        long calculatedOffset = recentSentExplanationOffset - recentConsumedExplanationOffset + 1;

        log.info("Session ID: {}", httpSession.getId());
        log.info("Calculated Offset: {}", calculatedOffset);

        return ResponseEntity.ok(calculatedOffset);
    }


    // DEMO 문제 생성
    @PostMapping("/api/question/createQuestionAllRandom/LLaMA")
    public ResponseEntity<QuestionType> createDemoQuestion(HttpSession httpSession) throws InterruptedException, JsonProcessingException, ExecutionException {
        log.info("Session ID: {}", httpSession.getId());

        QuestionType questionType = QuestionType.getRandomQuestionType();

        String definition = questionService.getQuestionDefinition(questionType);
        String mainText = questionService.getRandomDefaultDataset();

        Long offset = kafkaService.makeQuestionFromKafka(new QuestionSubmitKafkaForm(questionType.toString(), definition, mainText), httpSession);
        httpSession.setAttribute("questionOffset", offset);

        log.info("now Offset from sent message : {}", offset);

        return ResponseEntity.ok(questionType);
    }


    // 무작위 문제 생성
    @PostMapping("/api/question/createRandom/LLaMA")
    public ResponseEntity<QuestionType> createDefaultQuestion(HttpSession httpSession, @RequestBody QuestionSubmitRawForm form) throws InterruptedException, JsonProcessingException, ExecutionException {

        QuestionType questionType = QuestionType.valueOf(form.getType());

        String definition = questionService.getQuestionDefinition(questionType);
        String mainText = questionService.getRandomDefaultDataset();

        Long offset = kafkaService.makeQuestionFromKafka(new QuestionSubmitKafkaForm(questionType.toString(), definition, mainText), httpSession);
        httpSession.setAttribute("questionOffset", offset);

        log.info("now Offset from sent message : {}", offset);

        return ResponseEntity.ok(questionType);
    }


    // 사용자 정의 지문 문제 생성
    @PostMapping("/api/question/create/LLaMA")
    public ResponseEntity<QuestionType> createCustomQuestion(HttpSession httpSession, @RequestBody QuestionSubmitRawForm form) throws InterruptedException, JsonProcessingException, ExecutionException {

        QuestionType questionType = QuestionType.valueOf(form.getType());

        String definition = questionService.getQuestionDefinition(questionType);
        String mainText = form.getMainText();

        Long offset = kafkaService.makeQuestionFromKafka(new QuestionSubmitKafkaForm(questionType.toString(), definition, mainText), httpSession);
        httpSession.setAttribute("questionOffset", offset);

        log.info("now Offset from sent message : {}", offset);

        return ResponseEntity.ok(questionType);
    }


    @PostMapping("/api/question/create")
    public ResponseEntity<?> getQuestion(HttpSession httpSession, @RequestBody QuestionType questionType) {

        QuestionResponseRawForm response = kafkaService.receiveQuestionFromKafka(httpSession);
        if (response == null) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body("아직 문제가 만들어지지 않았습니다.");
        }

        response.setQuestionType(questionType);

        return ResponseEntity.ok(response);
    }


    // 해설 생성
    @PostMapping("/api/question/explanation/LLaMA")
    public ResponseEntity<String> createExplanation(HttpSession httpSession, @RequestBody QuestionResponseRawForm form) throws ExecutionException, InterruptedException, JsonProcessingException {

        Long offset = kafkaService.makeExplanationFromKafka(form, httpSession);
        httpSession.setAttribute("explanationOffset", offset);

        return ResponseEntity.ok("해설 생성 요청 전송 완료");
    }

    @PostMapping("/api/question/explanation/create")
    public ResponseEntity<?> getExplanation(HttpSession httpSession, @RequestBody QuestionResponseRawForm form) {

        ExplanationResponseRawForm explanation = kafkaService.receiveExplanationFromKafka(httpSession);
        if (explanation == null) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body("아직 해설이 만들어지지 않았습니다.");
        }

        log.info(explanation.toString());

        QuestionDto question = QuestionDto.builder()
                .title(form.getTitle())
                .questionType(form.getQuestionType())
                .mainText(form.getMainText())
                .choices(form.getChoices().stream().toList())
                .answer(explanation.getAnswer())
                .translation(explanation.getTranslation())
                .explanation(explanation.getExplanation())
                .build();

        log.info(question.toString());

        log.info("해설 Type : {} 생성 완료.", form.getQuestionType());

        return ResponseEntity.ok(question);
    }
}