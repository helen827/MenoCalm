package com.livemore.api;

import com.livemore.api.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
@EnableAsync
public class LiveMoreApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(LiveMoreApiApplication.class, args);
    }
}
