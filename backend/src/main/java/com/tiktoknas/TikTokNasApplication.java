package com.tiktoknas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class TikTokNasApplication {
    public static void main(String[] args) {
        SpringApplication.run(TikTokNasApplication.class, args);
    }
}
