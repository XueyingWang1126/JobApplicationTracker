package com.xueying.jobapplicationtracker;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot entry point for job-application-tracker.
 */
@SpringBootApplication
@MapperScan("com.xueying.jobapplicationtracker.mapper")
public class JobApplicationTrackerApplication {
    public static void main(String[] args) {
        SpringApplication.run(JobApplicationTrackerApplication.class, args);
    }
}

