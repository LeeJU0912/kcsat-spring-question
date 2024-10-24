package hpclab.kcsatspringquestion.questionGenerator.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import hpclab.kcsatspringquestion.questionGenerator.domain.QuestionType;
import hpclab.kcsatspringquestion.kafka.KafkaService;
import hpclab.kcsatspringquestion.questionGenerator.dto.*;
import hpclab.kcsatspringquestion.questionGenerator.service.QuestionService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

        log.info("Session ID: {}", httpSession.getId());
        log.info("recentSentQuestionOffset: {}", recentSentQuestionOffset);
        log.info("recentConsumedQuestionOffset: {}", recentConsumedQuestionOffset);

        return ResponseEntity.ok(recentSentQuestionOffset - recentConsumedQuestionOffset + 1);
    }


    // 현재 offset 찾기
    @GetMapping("/api/question/explanationOffsetGap")
    public ResponseEntity<Long> getExplanationOffset(HttpSession httpSession) {
        long recentSentQuestionOffset = (long) httpSession.getAttribute("explanationOffset");
        long recentConsumedExplanationOffset = kafkaService.getRecentConsumedExplanationOffset(httpSession);

        return ResponseEntity.ok(recentSentQuestionOffset - recentConsumedExplanationOffset + 1);
    }


    // DEMO 문제 생성
    @PostMapping("/api/question/createQuestionAllRandom/LLaMA")
    public ResponseEntity<QuestionResponseRawForm> createDemoQuestion(HttpSession httpSession) throws InterruptedException, JsonProcessingException, ExecutionException {
        log.info("Session ID: {}", httpSession.getId());

        QuestionType questionType = QuestionType.getRandomQuestionType();

        String definition = questionService.getQuestionDefinition(questionType);
        String mainText = questionService.getRandomDefaultDataset();

        Long offset = kafkaService.makeQuestionFromKafka(new QuestionSubmitKafkaForm(definition, mainText), httpSession);
        httpSession.setAttribute("questionOffset", offset);

        log.info("now Offset from send message : {}", offset);

        QuestionResponseRawForm response = kafkaService.receiveQuestionFromKafka(httpSession);
        response.setQuestionType(questionType);

        log.info("DEMO 문제 Type : {} 생성 완료.", response.getQuestionType());

        return ResponseEntity.ok(response);
    }


    // 무작위 문제 생성
    @PostMapping("/api/question/createRandom/LLaMA")
    public ResponseEntity<QuestionResponseRawForm> createDefaultQuestion(HttpSession httpSession, @RequestBody QuestionSubmitRawForm form) throws InterruptedException, JsonProcessingException, ExecutionException {

        QuestionType questionType = QuestionType.valueOf(form.getType());

        String definition = questionService.getQuestionDefinition(questionType);
        String mainText = questionService.getRandomDefaultDataset();

        Long offset = kafkaService.makeQuestionFromKafka(new QuestionSubmitKafkaForm(definition, mainText), httpSession);
        httpSession.setAttribute("questionOffset", offset);

        QuestionResponseRawForm response = kafkaService.receiveQuestionFromKafka(httpSession);
        response.setQuestionType(questionType);

        log.info("기출 문제 Type : {} 생성 완료.", response.getQuestionType());

        return ResponseEntity.ok(response);
    }


    // 사용자 정의 지문 문제 생성
    @PostMapping("/api/question/create/LLaMA")
    public ResponseEntity<QuestionResponseRawForm> createCustomQuestion(HttpSession httpSession, @RequestBody QuestionSubmitRawForm form) throws InterruptedException, JsonProcessingException, ExecutionException {

        QuestionType questionType = QuestionType.valueOf(form.getType());

        String definition = questionService.getQuestionDefinition(questionType);
        String mainText = form.getMainText();

        Long offset = kafkaService.makeQuestionFromKafka(new QuestionSubmitKafkaForm(definition, mainText), httpSession);
        httpSession.setAttribute("questionOffset", offset);

        QuestionResponseRawForm response = kafkaService.receiveQuestionFromKafka(httpSession);
        response.setQuestionType(questionType);

        log.info("외부 지문 문제 Type : {} 생성 완료.", questionType);

        return ResponseEntity.ok(response);
    }


    // 해설 생성
    @PostMapping("/api/question/explanation/LLaMA")
    public ResponseEntity<QuestionDto> createExplanation(HttpSession httpSession, @RequestBody QuestionResponseRawForm form) throws ExecutionException, InterruptedException, JsonProcessingException {

        Long offset = kafkaService.makeExplanationFromKafka(form, httpSession);
        httpSession.setAttribute("explanationOffset", offset);

        ExplanationResponseRawForm explanation = kafkaService.receiveExplanationFromKafka(httpSession);

        QuestionDto question = QuestionDto.builder()
                .title(form.getTitle())
                .questionType(form.getQuestionType())
                .mainText(form.getMainText())
                .choices(form.getChoices().stream().toList())
                .answer(form.getAnswer())
                .translation(explanation.getTranslation())
                .explanation(explanation.getExplanation())
                .build();

        log.info("해설 Type : {} 생성 완료.", form.getQuestionType());

        return ResponseEntity.ok(question);
    }
}