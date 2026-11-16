package app.datasets.config;

import java.util.concurrent.ExecutorService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Executors {

    /** Locks run one at a time, like a single database copy job. */
    @Bean(destroyMethod = "shutdownNow")
    public ExecutorService lockExecutor() {
        return java.util.concurrent.Executors.newSingleThreadExecutor(Thread.ofPlatform().name("lock-", 0).factory());
    }

    @Bean(destroyMethod = "shutdownNow")
    public ExecutorService inferExecutor() {
        return java.util.concurrent.Executors.newFixedThreadPool(2, Thread.ofPlatform().name("infer-", 0).factory());
    }
}
