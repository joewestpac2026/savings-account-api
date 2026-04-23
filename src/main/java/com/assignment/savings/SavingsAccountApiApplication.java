package com.assignment.savings;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class SavingsAccountApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(SavingsAccountApiApplication.class, args);
    }

}
