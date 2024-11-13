package hpclab.kcsatspringquestion.kafka;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

public class KafkaOffsetChecker {

    private static final AdminClient adminClient;

    static {
        Properties config = new Properties();
        config.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:9092");
        adminClient = AdminClient.create(config);
    }

    // 특정 소비자 그룹의 오프셋 조회 메서드
    public long getCommittedOffset(String groupId, String topic, int partition) {
        TopicPartition topicPartition = new TopicPartition(topic, partition);
        try {
            // Kafka Admin API로 소비자 그룹의 오프셋 조회
            Map<TopicPartition, OffsetAndMetadata> offsets =
                    adminClient.listConsumerGroupOffsets(groupId)
                            .partitionsToOffsetAndMetadata().get();

            OffsetAndMetadata offsetAndMetadata = offsets.get(topicPartition);

            if (offsetAndMetadata != null) {
                System.out.printf("Committed offset for %s-%d: %d%n",
                        topic, partition, offsetAndMetadata.offset());
                return offsetAndMetadata.offset();
            } else {
                System.out.printf("No committed offset found for %s-%d%n",
                        topic, partition);
                return -1;
            }
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException("Error while fetching offsets", e);
        }
    }
}
