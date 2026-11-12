package io.github.screeningloop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(proxyBeanMethods = false)
@ConfigurationPropertiesScan
public final class ScreeningLoopApplication {

    private ScreeningLoopApplication() {
    }

    public static void main(String[] args) {
        SpringApplication.run(ScreeningLoopApplication.class, args);
    }
}
