package hpclab.kcsatspringquestion.redis;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 기본 정보를 설정하는 클래스입니다.
 */
@Configuration
public class RedisConfig {

    /**
     * Redis 기본 호스트 주소
     */
    @Value("${spring.data.redis.host}")
    private String host;

    /**
     * Redis 기본 포트 주소
     */
    @Value("${spring.data.redis.port}")
    private int port;

    /**
     * RedisConnection 스프링 Bean입니다.
     *
     * @return LettuceConnectionFactory로 커넥션 정의
     */
    @Primary
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory(host, port);
    }

    /**
     * RedisTemplate 설정 Bean입니다.
     *
     * - Key: 문자열(String) 형식으로 직렬화
     * - Value: Jackson을 사용하여 JSON <-> Java 객체 간 직렬화/역직렬화
     *
     * 주로 Java 객체를 JSON 형태로 Redis에 저장하고, 다시 읽을 때 문자열로 변환됩니다.
     */
    @Bean
    public RedisTemplate<?, ?> redisTemplate() {
        RedisTemplate<?, ?> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory());
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new Jackson2JsonRedisSerializer<>(String.class));
        return redisTemplate;
    }
}
