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

@Getter
@Repository
public class QuestionMemoryRepository {

    private final List<String> defaultDatasets;
    private final TreeMap<QuestionType, String> questionDefinitions;
    private final TreeMap<QuestionType, String> explanationDefinitions;

    // 문제 Definition 로드
    public String getQuestionDefinition(QuestionType type) {
        return questionDefinitions.get(type);
    }

    // 해설 Definition 로드
    public String getExplanationDefinition(QuestionType type) {
        return explanationDefinitions.get(type);
    }

    // 초기 init
    public QuestionMemoryRepository() throws Exception {
        this.defaultDatasets = makeDatasets();
        this.questionDefinitions = makeQuestionDefinitions();
        this.explanationDefinitions = makeExplanationDefinitions();
    }


    @SuppressWarnings("unchecked")
    private TreeMap<QuestionType, String> makeQuestionDefinitions() throws Exception {

        JSONObject object = getData("static/dataset/K-SAT_questionDefinition.json");

        QuestionType[] values = QuestionType.values();
        ArrayList<String> definition = (ArrayList<String>) object.get("definition");

        TreeMap<QuestionType, String> definitions = new TreeMap<>();

        for (int i = 0; i < values.length; i++) {
            definitions.put(values[i], definition.get(i));
        }

        return definitions;
    }


    @SuppressWarnings("unchecked")
    private TreeMap<QuestionType, String> makeExplanationDefinitions() throws Exception {

        JSONObject object = getData("static/dataset/K-SAT_explanationDefinition.json");

        QuestionType[] values = QuestionType.values();
        ArrayList<String> definition = (ArrayList<String>) object.get("definition");

        TreeMap<QuestionType, String> definitions = new TreeMap<>();

        for (int i = 0; i < values.length; i++) {
            definitions.put(values[i], definition.get(i));
        }

        return definitions;
    }


    private List<String> makeDatasets() throws Exception {
        JSONObject object = getData("static/dataset/K-SAT_dataset.json");
        return (ArrayList<String>) object.get("dataset");
    }


    private JSONObject getData(String name) throws IOException, ParseException {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(name);

        InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);

        JSONParser parser = new JSONParser();
        JSONObject object = (JSONObject) parser.parse(reader);

        reader.close();
        return object;
    }
}
