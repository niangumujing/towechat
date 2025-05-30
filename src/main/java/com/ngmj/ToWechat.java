package com.ngmj;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ToWechat {
    public static void main(String[] args) {
        org.springframework.boot.SpringApplication.run(ToWechat.class, args);
    }
}
