package hpclab.kcsatspringquestion.questionGenerator.repository;

import hpclab.kcsatspringquestion.questionGenerator.domain.QuestionType;
import lombok.Getter;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.stereotype.Repository;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * 문제 제작을 위해 데이터셋을 준비하는 로직을 구현한 클래스입니다.
 * 데이터셋은 파일로 존재하고, 초기 구동시에 메모리에 올라갑니다.
 */
@Getter
@Repository
public class QuestionMemoryRepository {

    private static final String QUESTION_SOURCE = "static/dataset/K-SAT_questionDefinition.json";
    private static final String EXPLANATION_SOURCE = "static/dataset/K-SAT_explanationDefinition.json";
    private static final String DATASET_SOURCE = "static/dataset/K-SAT_dataset.json";

    private static final String DATASET = "dataset";
    private static final String DEFINITION = "definition";

    private final List<String> defaultDatasets;
    private final TreeMap<QuestionType, String> questionDefinitions;
    private final TreeMap<QuestionType, String> explanationDefinitions;

    /**
     * 문제 유형에 맞는 Definition을 불러오는 메서드입니다.
     *
     * @param type 문제 유형
     * @return Definition
     */
    public String getQuestionDefinition(QuestionType type) {
        return questionDefinitions.get(type);
    }

    /**
     * 해설 유형에 맞는 Definition을 불러오는 메서드입니다.
     *
     * @param type 해설 유형
     * @return Definition
     */
    public String getExplanationDefinition(QuestionType type) {
        return explanationDefinitions.get(type);
    }

    /**
     * 스프링 서버 초기 기동시, 데이터셋을 메모리에 올리는 메서드입니다.
     *
     * @throws Exception 데이터를 읽지 못하는 경우, 오류를 내고 종료합니다.
     */
    // 초기 init
    public QuestionMemoryRepository() throws Exception {
        this.defaultDatasets = makeDatasets();
        this.questionDefinitions = makeQuestionDefinitions();
        this.explanationDefinitions = makeExplanationDefinitions();
    }

    /**
     * 문제 Definition 목록을 메모리에 불러오는 메서드입니다.
     *
     * @return 문제 유형에 대해 Definition이 Map 형태로 매칭됩니다.
     * @throws Exception 파일을 불러오지 못하는 경우, 오류를 내고 종료합니다.
     */
    @SuppressWarnings("unchecked")
    private TreeMap<QuestionType, String> makeQuestionDefinitions() throws Exception {

        JSONObject object = getData(QUESTION_SOURCE);

        QuestionType[] values = QuestionType.values();
        ArrayList<String> definition = (ArrayList<String>) object.get(DEFINITION);

        TreeMap<QuestionType, String> definitions = new TreeMap<>();

        for (int i = 0; i < values.length; i++) {
            definitions.put(values[i], definition.get(i));
        }

        return definitions;
    }

    /**
     * 해설 Definition 목록을 메모리에 불러오는 메서드입니다.
     *
     * @return 해설 유형에 대해 Definition이 Map 형태로 매칭됩니다.
     * @throws Exception 파일을 불러오지 못하는 경우, 오류를 내고 종료합니다.
     */
    @SuppressWarnings("unchecked")
    private TreeMap<QuestionType, String> makeExplanationDefinitions() throws Exception {

        JSONObject object = getData(EXPLANATION_SOURCE);

        QuestionType[] values = QuestionType.values();
        ArrayList<String> definition = (ArrayList<String>) object.get(DEFINITION);

        TreeMap<QuestionType, String> definitions = new TreeMap<>();

        for (int i = 0; i < values.length; i++) {
            definitions.put(values[i], definition.get(i));
        }

        return definitions;
    }

    /**
     * 기출 문제 지문 목록을 메모리에 불러오는 메서드입니다.
     *
     * @return 지문들이 ArrayList 형태로 저장됩니다.
     * @throws Exception 파일을 불러오지 못하는 경우, 오류를 내고 종료합니다.
     */
    private List<String> makeDatasets() throws Exception {
        JSONObject object = getData(DATASET_SOURCE);
        return (ArrayList<String>) object.get(DATASET);
    }

    /**
     * 파일을 읽어오는 메서드입니다.
     *
     * @param path 파일 경로
     * @return 파일을 읽어 JSON 객체로 역직렬화시켜 반환합니다.
     * @throws IOException 파일을 불러오지 못하는 경우 발생합니다.
     * @throws ParseException JSON 파싱에 실패하는 경우 발생합니다.
     */
    private JSONObject getData(String path) throws IOException, ParseException {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(path);

        InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);

        JSONParser parser = new JSONParser();
        JSONObject object = (JSONObject) parser.parse(reader);

        reader.close();
        return object;
    }
}