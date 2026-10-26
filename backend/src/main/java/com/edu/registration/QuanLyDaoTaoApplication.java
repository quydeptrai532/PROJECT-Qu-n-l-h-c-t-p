package com.edu.registration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class QuanLyDaoTaoApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuanLyDaoTaoApplication.class, args);
    }
}
