package com.example.ticketmetricsapibackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Application entry point for ticketmetricsapibackend.
 * Ensures component scanning includes controllers under com.example.ticketmetricsapibackend.*
 */
@SpringBootApplication
public class ticketmetricsapibackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(ticketmetricsapibackendApplication.class, args);
	}

}
