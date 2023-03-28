package com.sahay.third.party;

import lombok.extern.java.Log;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@Log
@EnableAsync
@EnableScheduling
@SpringBootApplication
public class ThirdPartyApplication {
	public static void main(String[] args) {
		SpringApplication.run(ThirdPartyApplication.class, args);
	}
}
