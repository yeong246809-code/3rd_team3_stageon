package kr.co.stageon.ai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class AiClientConfig {

    @Bean(name = "stageonAiRestClient")
    RestClient stageonAiRestClient(
            @Value("${stageon.ai.gateway-url}") String gatewayUrl,
            @Value("${stageon.ai.api-key}") String apiKey,
            @Value("${stageon.ai.connect-timeout}") Duration connectTimeout,
            @Value("${stageon.ai.response-timeout}") Duration responseTimeout
    ) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(responseTimeout);

        return RestClient.builder()
                .requestFactory(requestFactory)
                .baseUrl(gatewayUrl)
                .defaultHeader(HttpHeaders.ACCEPT, "application/json")
                .defaultHeader("X-API-Key", apiKey)
                .build();
    }
}
