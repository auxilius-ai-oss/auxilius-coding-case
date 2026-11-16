package app.datasets.config;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app")
public record AppProperties(Path storageDir, boolean seed, Upload upload, Infer infer, Lock lock) {

    public record Upload(long maxBytes, long bytesPerSecond, double failureRate) {}

    public record Infer(long minMillis, long maxMillis, double failureRate) {}

    public record Lock(long millisPerFile, long millisPerThousandRows) {}
}
