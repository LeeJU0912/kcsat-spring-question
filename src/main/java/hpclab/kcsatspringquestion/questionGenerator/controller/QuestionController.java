package hpclab.kcsatspringquestion.questionGenerator.controller;

import hpclab.kcsatspringquestion.exception.ApiResponse;
import hpclab.kcsatspringquestion.exception.SuccessCode;
import hpclab.kcsatspringquestion.kafka.KafkaService;
import hpclab.kcsatspringquestion.questionGenerator.domain.QuestionType;
import hpclab.kcsatspringquestion.questionGenerator.dto.*;
import hpclab.kcsatspringquestion.questionGenerator.service.QuestionService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 문제,해설 생성 로직을 제어하는 컨트롤러 클래스입니다.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;
    private final KafkaService kafkaService;

    /**
     * 문제 생성시, UUID 할당을 위해 최초 1회 호출하는 메서드입니다.
     * UUID와 문제 생성 Topic을 미리 할당 받고, 해당 Topic에 문제 생성을 요청합니다.
     *
     * @param httpSession 회원 HTTP 세션 정보
     * @return 세션 UUID 값을 반환합니다.
     */
    @GetMapping("/api/question/firstQuestionContact")
    public ResponseEntity<ApiResponse<String>> createQuestionData(HttpSession httpSession) {
        log.info("First get Session ID: {}", httpSession.getId());

        // topic 설정
        String questionTopic = kafkaService.getQuestionTopic();

        // session에 topic 저장
        httpSession.setAttribute("questionTopic", questionTopic);

        return ResponseEntity.ok(new ApiResponse<>(true, httpSession.getId(), null, null));
    }

    /**
     * 해설 생성시, UUID 할당을 위해 최초 1회 호출하는 메서드입니다.
     * UUID와 해설 생성 Topic을 미리 할당 받고, 해당 Topic에 해설 생성을 요청합니다.
     *
     * @param httpSession 회원 HTTP 세션 정보
     * @return 세션 UUID 값을 반환합니다.
     */
    @GetMapping("/api/question/firstExplanationContact")
    public ResponseEntity<ApiResponse<String>> createExplanationData(HttpSession httpSession) {
        log.info("First get Session ID: {}", httpSession.getId());

        // topic 설정
        String explanationTopic = kafkaService.getExplanationTopic();

        // session에 topic 저장
        httpSession.setAttribute("explanationTopic", explanationTopic);

        return ResponseEntity.ok(new ApiResponse<>(true, httpSession.getId(), null, null));
    }

    /**
     * 문제 생성 요청 이후, 회원이 요청한 문제의 Offset과 최근에 Consume된 문제 Offset을 비교하여 반환하는 메서드입니다.
     * (회원 요청 문제 Offset - 가장 최근에 Consume된 문제 Offset + 1 = 회원이 문제를 받기까지 남은 메시지 수)
     *
     * @param httpSession 회원 HTTP 세션
     * @return 회원이 문제를 받기까지 남은 메시지 수를 반환합니다.
     */
    @GetMapping("/api/question/questionOffsetGap")
    public ResponseEntity<ApiResponse<Long>> getQuestionOffset(HttpSession httpSession) {
        long recentSentQuestionOffset = (long) httpSession.getAttribute("questionOffset");
        long recentConsumedQuestionOffset = kafkaService.getRecentConsumedQuestionOffset(httpSession);

        long calculatedOffset = recentSentQuestionOffset - recentConsumedQuestionOffset + 1;

        log.info("Session ID: {}", httpSession.getId());
        log.info("Calculated Offset: {}", calculatedOffset);

        return ResponseEntity.ok(new ApiResponse<>(true, calculatedOffset, null, null));
    }

    /**
     * 해설 생성 요청 이후, 회원이 요청한 해설의 Offset과 최근에 Consume된 해설 Offset을 비교하여 반환하는 메서드입니다.
     * (회원 요청 해설 Offset - 가장 최근에 Consume된 해설 Offset + 1 = 회원이 해설을 받기까지 남은 메시지 수)
     *
     * @param httpSession 회원 HTTP 세션
     * @return 회원이 해설을 받기까지 남은 메시지 수를 반환합니다.
     */
    @GetMapping("/api/question/explanationOffsetGap")
    public ResponseEntity<ApiResponse<Long>> getExplanationOffset(HttpSession httpSession) {
        long recentSentExplanationOffset = (long) httpSession.getAttribute("explanationOffset");
        long recentConsumedExplanationOffset = kafkaService.getRecentConsumedExplanationOffset(httpSession);

        long calculatedOffset = recentSentExplanationOffset - recentConsumedExplanationOffset + 1;

        log.info("Session ID: {}", httpSession.getId());
        log.info("Calculated Offset: {}", calculatedOffset);

        return ResponseEntity.ok(new ApiResponse<>(true, calculatedOffset, null, null));
    }

    /**
     * DEMO 문제 생성 요청 - 기출 문제 지문 중 무작위로 한 가지를 골라 랜덤한 유형으로 문제를 제작하는 메서드입니다.
     *
     * @param httpSession 회원 HTTP 세션
     * @return 제작될 문제 유형을 반환합니다.
     */
    @PostMapping("/api/question/createQuestionAllRandom/LLaMA")
    public ResponseEntity<ApiResponse<QuestionType>> createDemoQuestion(HttpSession httpSession) {
        log.info("Session ID: {}", httpSession.getId());

        QuestionType questionType = QuestionType.getRandomQuestionType();

        String definition = questionService.getQuestionDefinition(questionType);
        String mainText = questionService.getRandomDefaultDataset();

        Long offset = kafkaService.makeQuestionFromKafka(new QuestionSubmitKafkaForm(questionType.toString(), definition, mainText), httpSession);
        httpSession.setAttribute("questionOffset", offset);

        log.info("now Offset from sent message : {}", offset);

        return ResponseEntity.ok(new ApiResponse<>(true, questionType, null, null));
    }

    /**
     * 무작위 문제 생성 요청 - 기출 문제 지문 중 무작위로 한 가지를 골라 사용자가 고른 유형으로 문제를 제작하는 메서드입니다.
     * @param httpSession 회원 HTTP 세션
     * @param form 이 메서드에서는 사용자가 지정한 문제 유형만을 참고합니다.
     * @return 제작될 문제 유형을 반환합니다.
     */
    @PostMapping("/api/question/createRandom/LLaMA")
    public ResponseEntity<ApiResponse<QuestionType>> createDefaultQuestion(HttpSession httpSession, @RequestBody QuestionSubmitRawForm form) {

        QuestionType questionType = QuestionType.valueOf(form.getType());

        String definition = questionService.getQuestionDefinition(questionType);
        String mainText = questionService.getRandomDefaultDataset();

        Long offset = kafkaService.makeQuestionFromKafka(new QuestionSubmitKafkaForm(questionType.toString(), definition, mainText), httpSession);
        httpSession.setAttribute("questionOffset", offset);

        log.info("now Offset from sent message : {}", offset);

        return ResponseEntity.ok(new ApiResponse<>(true, questionType, null, null));
    }

    /**
     * 사용자 정의 지문 문제 생성 요청 - 사용자가 입력한 지문에 대해 사용자가 유형을 선택하여 문제를 제작하는 메서드입니다.
     *
     * @param httpSession 회원 HTTP 세션
     * @param form 이 메서드에서는 사용자가 작성한 본문, 지정한 문제 유형 데이터를 모두 참고합니다.
     * @return 제작될 문제 유형을 반환합니다.
     */
    @PostMapping("/api/question/create/LLaMA")
    public ResponseEntity<ApiResponse<QuestionType>> createCustomQuestion(HttpSession httpSession, @RequestBody QuestionSubmitRawForm form) {

        QuestionType questionType = QuestionType.valueOf(form.getType());

        String definition = questionService.getQuestionDefinition(questionType);
        String mainText = form.getMainText();

        Long offset = kafkaService.makeQuestionFromKafka(new QuestionSubmitKafkaForm(questionType.toString(), definition, mainText), httpSession);
        httpSession.setAttribute("questionOffset", offset);

        log.info("now Offset from sent message : {}", offset);

        return ResponseEntity.ok(new ApiResponse<>(true, questionType, null, null));
    }

    /**
     * 생성 요청한 문제가 다 만들어지면, 반환 요청하는 메서드입니다.
     *
     * @param httpSession 회원 HTTP 세션
     * @param questionType 제작 요청한 문제 유형. 만들어진 문제 객체에 추가합니다.
     * @return 문제가 다 만들어졌다면 문제 정보를 반환합니다. 다 만들어지지 않았다면 NO_CONTENT를 반환합니다.
     */
    @PostMapping("/api/question/create")
    public ResponseEntity<ApiResponse<QuestionResponseRawForm>> getQuestion(HttpSession httpSession, @RequestBody QuestionType questionType) {

        QuestionResponseRawForm response = kafkaService.receiveQuestionFromKafka(httpSession);

        response.setQuestionType(questionType);

        return ResponseEntity.ok(new ApiResponse<>(true, response, null, null));
    }

    /**
     * 문제 해설 생성 요청 메서드입니다.
     * 문제가 다 만들어졌을 경우, 해설 생성을 위해 추가로 요청하여 해설을 제작하는 메서드입니다.
     *
     * @param httpSession 회원 HTTP 세션
     * @param form 생성된 문제 정보. 이 데이터를 기반으로 문제 해설을 생성합니다.
     * @return 해설 생성에 성공한다면 OK를 반환합니다.
     */
    // 해설 생성
    @PostMapping("/api/question/explanation/LLaMA")
    public ResponseEntity<ApiResponse<Void>> createExplanation(HttpSession httpSession, @RequestBody QuestionResponseRawForm form) {

        Long offset = kafkaService.makeExplanationFromKafka(form, httpSession);
        httpSession.setAttribute("explanationOffset", offset);

        return ResponseEntity.ok(new ApiResponse<>(true, null, SuccessCode.MESSAGE_SEND_SUCCESS.getCode(), SuccessCode.MESSAGE_SEND_SUCCESS.getMessage()));
    }

    /**
     * 생성 요청한 해설이 다 만들어지면, 반환 요청하는 메서드입니다.
     *
     * @param httpSession 회원 HTTP 세션
     * @param form 제작 완료된 문제 정보. 만들어진 해설 객체와 결합합니다.
     * @return 해설이 다 만들어졌다면 해설 정보를 반환합니다. 다 만들어지지 않았다면 NO_CONTENT를 반환합니다.
     */
    @PostMapping("/api/question/explanation/create")
    public ResponseEntity<ApiResponse<QuestionDto>> getExplanation(HttpSession httpSession, @RequestBody QuestionResponseRawForm form) {

        ExplanationResponseRawForm explanation = kafkaService.receiveExplanationFromKafka(httpSession);

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

        return ResponseEntity.ok(new ApiResponse<>(true, question, null, null));
    }
}