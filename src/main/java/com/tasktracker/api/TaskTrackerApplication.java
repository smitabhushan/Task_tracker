package com.tasktracker.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class TaskTrackerApplication {
    public static void main(String[] args) {
        SpringApplication.run(TaskTrackerApplication.class, args);
    }
}
