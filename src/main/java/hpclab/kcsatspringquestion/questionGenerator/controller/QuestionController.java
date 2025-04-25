package hpclab.kcsatspringquestion.questionGenerator.controller;

import hpclab.kcsatspringquestion.exception.ApiResponse;
import hpclab.kcsatspringquestion.exception.SuccessCode;
import hpclab.kcsatspringquestion.kafka.KafkaService;
import hpclab.kcsatspringquestion.questionGenerator.domain.QuestionType;
import hpclab.kcsatspringquestion.questionGenerator.dto.*;
import hpclab.kcsatspringquestion.questionGenerator.service.QuestionService;
import hpclab.kcsatspringquestion.util.JWTUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static hpclab.kcsatspringquestion.util.JWTUtil.USER_EMAIL;

/**
 * 문제,해설 생성 로직을 제어하는 컨트롤러 클래스입니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/question")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;
    private final KafkaService kafkaService;

    private final JWTUtil jwtUtil;

    /**
     * 문제 생성시, Topic 할당을 위해 최초 1회 호출하는 메서드입니다.
     * 문제 생성 Topic을 미리 할당 받고, 해당 Topic에 문제 생성을 요청합니다.
     *
     * @param token 회원 JWT 정보
     * @return 할당된 Question Topic 값을 반환합니다.
     */
    @GetMapping("/firstQuestionContact")
    public ResponseEntity<ApiResponse<String>> createQuestionData(@RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
        String email = jwtUtil.getClaims(token).get(USER_EMAIL).toString();

        // topic 설정
        String questionTopic = kafkaService.getQuestionTopic();

        kafkaService.setUserQuestionTopic(email, questionTopic);

        return ResponseEntity.ok(new ApiResponse<>(true, questionTopic, null, null));
    }

    /**
     * 해설 생성시, Topic 할당을 위해 최초 1회 호출하는 메서드입니다.
     * 해설 생성 Topic을 미리 할당 받고, 해당 Topic에 해설 생성을 요청합니다.
     *
     * @param token 회원 JWT 정보
     * @return 할당된 Explanation Topic 값을 반환합니다.
     */
    @GetMapping("/firstExplanationContact")
    public ResponseEntity<ApiResponse<String>> createExplanationData(@RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
        String email = jwtUtil.getClaims(token).get(USER_EMAIL).toString();

        // topic 설정
        String explanationTopic = kafkaService.getExplanationTopic();

        kafkaService.setUserExplanationTopic(email, explanationTopic);

        return ResponseEntity.ok(new ApiResponse<>(true, explanationTopic, null, null));
    }

    /**
     * 문제 생성 요청 이후, 최근에 Consume된 문제 Offset을 반환하는 메서드입니다.
     * (프론트엔드에서 최초 요청 Offset과 비교하여 대기열 구현 가능.)
     *
     * @param token 회원 JWT 정보
     * @return 현재 Topic의 최근 Consume Offset을 반환합니다.
     */
    @GetMapping("/questionNowOffset")
    public ResponseEntity<ApiResponse<Long>> getQuestionOffset(@RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
        String email = jwtUtil.getClaims(token).get(USER_EMAIL).toString();

        long offset = kafkaService.getRecentConsumedQuestionOffset(email);

        return ResponseEntity.ok(new ApiResponse<>(true, offset, null, null));
    }

    /**
     * 해설 생성 요청 이후, 최근에 Consume된 해설 Offset을 반환하는 메서드입니다.
     * (프론트엔드에서 최초 요청 Offset과 비교하여 대기열 구현 가능.)
     *
     * @param token 회원 JWT 정보
     * @return 현재 Topic의 최근 Consume Offset을 반환합니다.
     */
    @GetMapping("/explanationNowOffset")
    public ResponseEntity<ApiResponse<Long>> getExplanationOffset(@RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
        String email = jwtUtil.getClaims(token).get(USER_EMAIL).toString();

        long offset = kafkaService.getRecentConsumedExplanationOffset(email);

        return ResponseEntity.ok(new ApiResponse<>(true, offset, null, null));
    }

    /**
     * DEMO 문제 생성 요청 - 기출 문제 지문 중 무작위로 한 가지를 골라 랜덤한 유형으로 문제를 제작하는 메서드입니다.
     *
     * @param token 회원 JWT 정보
     * @return 제작 요청된 문제의 Offset을 반환합니다.
     */
    @PostMapping("/allRandom")
    public ResponseEntity<ApiResponse<Long>> createDemoQuestion(@RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
        String email = jwtUtil.getClaims(token).get(USER_EMAIL).toString();

        QuestionType questionType = QuestionType.getRandomQuestionType();

        String definition = questionService.getQuestionDefinition(questionType);
        String mainText = questionService.getRandomDefaultDataset();

        Long offset = kafkaService.makeQuestionFromKafka(new QuestionSubmitKafkaForm(questionType.toString(), definition, mainText), email);

        log.info("now Offset from sent message : {}", offset);

        return ResponseEntity.ok(new ApiResponse<>(true, offset, null, null));
    }

    /**
     * 무작위 문제 생성 요청 - 기출 문제 지문 중 무작위로 한 가지를 골라 사용자가 고른 유형으로 문제를 제작하는 메서드입니다.
     *
     * @param token 회원 JWT 정보
     * @param form 이 메서드에서는 사용자가 지정한 문제 유형만을 참고합니다.
     * @return 제작 요청된 문제의 Offset을 반환합니다.
     */
    @PostMapping("/random")
    public ResponseEntity<ApiResponse<Long>> createDefaultQuestion(@RequestHeader(HttpHeaders.AUTHORIZATION) String token, @RequestBody QuestionSubmitRawForm form) {
        String email = jwtUtil.getClaims(token).get(USER_EMAIL).toString();

        QuestionType questionType = QuestionType.valueOf(form.getType());

        String definition = questionService.getQuestionDefinition(questionType);
        String mainText = questionService.getRandomDefaultDataset();

        Long offset = kafkaService.makeQuestionFromKafka(new QuestionSubmitKafkaForm(questionType.toString(), definition, mainText), email);

        log.info("now Offset from sent message : {}", offset);

        return ResponseEntity.ok(new ApiResponse<>(true, offset, null, null));
    }

    /**
     * 사용자 정의 지문 문제 생성 요청 - 사용자가 입력한 지문에 대해 사용자가 유형을 선택하여 문제를 제작하는 메서드입니다.
     *
     * @param token 회원 JWT 정보
     * @param form 이 메서드에서는 사용자가 작성한 본문, 지정한 문제 유형 데이터를 모두 참고합니다.
     * @return 제작 요청된 문제의 Offset을 반환합니다.
     */
    @PostMapping("/question")
    public ResponseEntity<ApiResponse<Long>> createCustomQuestion(@RequestHeader(HttpHeaders.AUTHORIZATION) String token, @RequestBody QuestionSubmitRawForm form) {
        String email = jwtUtil.getClaims(token).get(USER_EMAIL).toString();

        QuestionType questionType = QuestionType.valueOf(form.getType());

        String definition = questionService.getQuestionDefinition(questionType);
        String mainText = form.getMainText();

        Long offset = kafkaService.makeQuestionFromKafka(new QuestionSubmitKafkaForm(questionType.toString(), definition, mainText), email);

        log.info("now Offset from sent message : {}", offset);

        return ResponseEntity.ok(new ApiResponse<>(true, offset, null, null));
    }

    /**
     * 생성 요청한 문제가 다 만들어지면, 반환 요청하는 메서드입니다.
     *
     * @param token 회원 JWT 정보
     * @return 문제가 다 만들어졌다면 문제 정보를 반환합니다.
     */
    @GetMapping("/question")
    public ResponseEntity<ApiResponse<QuestionResponseRawForm>> getQuestion(@RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
        String email = jwtUtil.getClaims(token).get(USER_EMAIL).toString();

        QuestionResponseRawForm response = kafkaService.receiveQuestionFromKafka(email);

        return ResponseEntity.ok(new ApiResponse<>(true, response, null, null));
    }

    /**
     * 문제 해설 생성 요청 메서드입니다.
     * 문제가 다 만들어졌을 경우, 해설 생성을 위해 추가로 요청하여 해설을 제작하는 메서드입니다.
     *
     * @param token 회원 JWT 정보
     * @param form 생성된 문제 정보. 이 데이터를 기반으로 문제 해설을 생성합니다.
     * @return 제작 요청된 해설의 Offset을 반환합니다.
     */
    @PostMapping("/explanation")
    public ResponseEntity<ApiResponse<Long>> createExplanation(@RequestHeader(HttpHeaders.AUTHORIZATION) String token, @RequestBody QuestionResponseRawForm form) {
        String email = jwtUtil.getClaims(token).get(USER_EMAIL).toString();

        questionService.saveQuestionResult(email, form);

        Long offset = kafkaService.makeExplanationFromKafka(form, email);

        return ResponseEntity.ok(new ApiResponse<>(true, offset, SuccessCode.MESSAGE_SEND_SUCCESS.getCode(), SuccessCode.MESSAGE_SEND_SUCCESS.getMessage()));
    }

    /**
     * 생성 요청한 해설이 다 만들어지면, 반환 요청하는 메서드입니다.
     *
     * @param token 회원 JWT 정보
     * @return 해설이 다 만들어졌다면 해설 정보를 반환합니다.
     */
    @GetMapping("/explanation")
    public ResponseEntity<ApiResponse<QuestionDto>> getExplanation(@RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
        String email = jwtUtil.getClaims(token).get(USER_EMAIL).toString();

        ExplanationResponseRawForm explanation = kafkaService.receiveExplanationFromKafka(email);

        QuestionDto question = questionService.mergeWithExplanation(email, explanation);

        return ResponseEntity.ok(new ApiResponse<>(true, question, null, null));
    }
}