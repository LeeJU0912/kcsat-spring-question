package hpclab.kcsatspringquestion.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;

import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Kafka Offset을 실시간으로 체크할 수 있는 클래스입니다.
 * 이 클래스를 이용하여 실시간 대기열을 구현할 수 있습니다.
 */
@Slf4j
@RequiredArgsConstructor
public class KafkaOffsetChecker {

    /**
     * Kafka AdminClient를 사용하여 Offset을 얻는 용도로 활용합니다.
     *
     * <p>AdminClient 사용 용도</p>
     * <ul>
     *     <li>토픽 목록 조회 / 생성 / 삭제</li>
     *     <li>파티션 수 변경</li>
     *     <li>컨슈머 그룹 목록/상태 조회</li>
     *     <li>오프셋(offset) 조회</li>
     *     <li>브로커 정보 조회</li>
     * </ul>
     */
    private final AdminClient adminClient;

    /**
     * 특정 Topic에 대한 Group의 Offset을 조회하는 메서드입니다.
     *
     * @param groupId Kafka Group ID
     * @param topic Kafka Topic
     * @param partition Topic Partition
     * @return 현재 Topic의 Offset을 반환합니다.
     */
    public long getCommittedOffset(String groupId, String topic, int partition) {

        TopicPartition topicPartition = new TopicPartition(topic, partition);

        try {
            Map<TopicPartition, OffsetAndMetadata> offsets =
                    adminClient.listConsumerGroupOffsets(groupId)
                            .partitionsToOffsetAndMetadata().get();

            OffsetAndMetadata offsetAndMetadata = offsets.get(topicPartition);

            if (offsetAndMetadata != null) {
                log.info("Committed offset for {}-{}: {}",
                        topic, partition, offsetAndMetadata.offset());
                return offsetAndMetadata.offset();
            } else {
                log.info("No committed offset found for {}-{}",
                        topic, partition);
                return -1;
            }
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException("Error while fetching offsets", e);
        }
    }
}
