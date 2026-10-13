package com.dinogo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DinogoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DinogoApplication.class, args);
	}

}
