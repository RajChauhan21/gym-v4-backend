package com.backend.gym_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
@EnableAsync
@SpringBootApplication
public class GymBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(GymBackendApplication.class, args);
	}

}
