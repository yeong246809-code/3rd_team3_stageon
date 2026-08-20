package kr.co.stageon.common.file;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "stageon.storage")
public class StorageProperties {
    private String type = "local";
    private String mediaUrlPrefix = "/media";
    private String localRoot = "uploads/media";
    private final S3 s3 = new S3();

    @Getter
    @Setter
    public static class S3 {
        private String bucket;
        private String region = "us-east-1";
        private String bannerPrefix = "image/banners";
        private String posterPrefix = "image/posters";
        private String filePrefix = "file";
    }
}
