package com.cosmetics.ecommerce;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class EcommerceApplication {

	public static void main(String[] args) {
		var context = SpringApplication.run(EcommerceApplication.class, args);

		Environment env = context.getEnvironment();
		String[] profiles = env.getActiveProfiles();
		System.out.println("\n========================================");
		System.out.println("Active Profile: " + (profiles.length > 0 ? profiles[0] : "default"));
		System.out.println("Server Port: " + env.getProperty("server.port"));
		System.out.println("Database: " + env.getProperty("spring.datasource.url"));
		System.out.println("========================================\n");
	}

}
