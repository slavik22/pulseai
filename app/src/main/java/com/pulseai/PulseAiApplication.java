package com.pulseai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync   // enables @Async for TransactionalEventListeners (AI generation, notifications)
public class PulseAiApplication {
    public static void main(String[] args) {
        SpringApplication.run(PulseAiApplication.class, args);
    }
}
