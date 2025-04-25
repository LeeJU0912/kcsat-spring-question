package hpclab.kcsatspringquestion.redis;


/**
 * Redis DB와 상호작용하는 키를 통합 관리하는 저장소 클래스입니다.
 */
public class RedisKeyUtil {

    // Kafka Topic 관련 키
    public static String questionTopic() {
        return "kafka:questionTopic";
    }

    public static String explanationTopic() {
        return "kafka:explanationTopic";
    }

    public static String questionTopicWithEmail(String email) {
        return "kafka:" + email + ":questionTopic";
    }

    public static String explanationTopicWithEmail(String email) {
        return "kafka:" + email + ":explanationTopic";
    }

    public static String questionRequestLock(String email) {
        return "kafka:" + email + ":questionRequestLock";
    }

    public static String explanationRequestLock(String email) {
        return "kafka:" + email + ":explanationRequestLock";
    }

    public static String questionMessage(String email) {
        return "kafka:" + email + ":questionMessage";
    }

    public static String explanationMessage(String email) {
        return "kafka:" + email + ":explanationMessage";
    }

    // Redis 관련 키
    public static String questionData(String email) {
        return "kafka:" + email + ":questionData";
    }

    public static String explanationData(String email) {
        return "kafka:" + email + ":explanationData";
    }
}
