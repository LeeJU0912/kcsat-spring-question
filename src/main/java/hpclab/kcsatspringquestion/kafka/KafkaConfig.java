package hpclab.kcsatspringquestion.kafka;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Kafka 설정 클래스입니다.
 * application.yaml로 auto-configuration도 가능하지만, 수동으로 작성해보았습니다. (향후 커스텀 가능)
 */
@EnableKafka
@Configuration
public class KafkaConfig {

    /**
     * Kafka 서버 URL
     */
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    /**
     * Kafka 그룹 ID
     * 같은 그룹으로 사용해야 하나의 메시지를 그룹 내의 한명이 처리합니다.
     */
    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    /**
     * Kafka ProducerFactory 설정 Bean입니다.
     * Producer 설정을 명시적으로 커스터마이징하려는 경우 사용됩니다.
     *
     * @return ProducerFactory
     */
    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new DefaultKafkaProducerFactory<>(props);
    }

    /**
     * KafkaTemplate 설정 Bean입니다.
     * ProducerFactory를 사용해 KafkaTemplate을 생성합니다.
     *
     * @return KafkaTemplate
     */
    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    /**
     * Kafka consumerFactory 설정 Bean입니다.
     * Consumer 설정을 명시적으로 커스터마이징하려는 경우 사용됩니다.
     *
     * @return ConsumerFactory
     */
    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> props = new HashMap<>();

        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * Kafka 리스너 컨테이너 팩토리 설정 Bean입니다.
     *
     * <p>Kafka 메시지를 수신하는 {@link org.springframework.kafka.annotation.KafkaListener}
     * 를 사용할 수 있도록 지원하는 리스너 컨테이너 팩토리를 생성합니다.</p>
     *
     * <p>이 Bean은 Spring Kafka가 자동으로 Kafka 컨슈머를 실행할 수 있게 하며,
     * 지정된 {@link ConsumerFactory}를 통해 KafkaConsumer 인스턴스를 생성합니다.</p>
     *
     * @return {@link ConcurrentKafkaListenerContainerFactory} 인스턴스
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }

    /**
     * Kafka AdminClient Bean을 생성합니다.
     *
     * <p>AdminClient는 Kafka 클러스터의 메타데이터(토픽, 파티션, 오프셋, 컨슈머 그룹 등)를 조회하거나
     * 관리하는 데 사용됩니다.</p>
     *
     * @return Kafka AdminClient 인스턴스
     */
    @Bean
    public AdminClient kafkaAdminClient() {
        Properties config = new Properties();
        config.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return AdminClient.create(config);
    }

    /**
     * KafkaOffsetChecker Bean을 생성합니다.
     *
     * <p>KafkaOffsetChecker는 AdminClient를 통해 Kafka 컨슈머 그룹의 커밋된 오프셋을 조회하는 데
     * 사용됩니다. Kafka 특정 Topic Group의 오프셋을 확인하는 데에 사용됩니다.</p>
     *
     * @param adminClient Kafka AdminClient Bean (Spring에서 주입)
     * @return KafkaOffsetChecker 인스턴스
     */
    @Bean
    public KafkaOffsetChecker kafkaOffsetChecker(AdminClient adminClient) {
        return new KafkaOffsetChecker(adminClient);
    }
}
