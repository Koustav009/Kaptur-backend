package com.koustav.kaptur;

import java.time.ZoneId;
import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class KapturApplication {

	public static final ZoneId IST = ZoneId.of("Asia/Kolkata");

	public static void main(String[] args) {
		// Setting default Time Zone
		TimeZone.setDefault(TimeZone.getTimeZone(IST));
		SpringApplication.run(KapturApplication.class, args);
	}

}
