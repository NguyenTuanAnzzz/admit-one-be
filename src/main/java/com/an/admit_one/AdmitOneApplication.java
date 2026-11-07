package com.an.admit_one;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AdmitOneApplication {

	public static void main(String[] args) {
		SpringApplication.run(AdmitOneApplication.class, args);
	}

}
