package kr.co.stageon.common.file;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfiguration {

    @Bean
    @ConditionalOnProperty(name = "stageon.storage.type", havingValue = "s3")
    S3Client s3Client(StorageProperties properties) {
        return S3Client.builder()
                .region(Region.of(properties.getS3().getRegion()))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}
