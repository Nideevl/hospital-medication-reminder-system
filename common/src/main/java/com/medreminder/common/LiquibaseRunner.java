package com.medreminder.common;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LiquibaseRunner {
    public static void main(String[] args) {
        SpringApplication.run(LiquibaseRunner.class, args);
    }
}