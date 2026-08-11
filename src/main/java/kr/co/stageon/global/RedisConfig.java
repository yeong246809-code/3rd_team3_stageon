package kr.co.stageon.global;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.TypedJsonJacksonCodec;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Value("${spring.data.redis.username}")
    private String redisUsername;

    // 비밀번호가 세팅되지 않았을 경우를 대비해 기본값(빈 문자열) 처리
    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // key와 value를 알아볼 수 있는 문자열(String)로 저장하기 위한 설정
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        return template;
    }

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule()); // LocalDateTime 등 Java 8 날짜 타입 지원
        config.setCodec(new TypedJsonJacksonCodec(Object.class, Object.class, objectMapper));

        // 1. 호스트와 포트 설정
        var singleServerConfig = config.useSingleServer()
                .setAddress("redis://" + redisHost + ":" + redisPort);

        // 2. 유저네임이 존재하면 설정 (빈 문자열이 아닐 경우)
        if (redisUsername != null && !redisUsername.isBlank()) {
            singleServerConfig.setUsername(redisUsername);
        }

        // 3. 비밀번호가 존재하면 설정 (빈 문자열이 아닐 경우)
        if (redisPassword != null && !redisPassword.isBlank()) {
            singleServerConfig.setPassword(redisPassword);
        }

        return Redisson.create(config);
    }
}