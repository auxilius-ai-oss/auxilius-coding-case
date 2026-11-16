package app.datasets;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class DatasetsApplication {

    public static void main(String[] args) {
        SpringApplication.run(DatasetsApplication.class, args);
    }
}
