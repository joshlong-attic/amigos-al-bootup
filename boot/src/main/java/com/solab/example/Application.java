package com.solab.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;


@SpringBootApplication
public class Application {

	@Bean
	AsyncServer protoServer(Lookups handler, Environment env) {
		return new AsyncServer(
			env.getProperty("proto.port", Integer.class, 8001),
			env.getProperty("proto.threads", Integer.class,
				Runtime.getRuntime().availableProcessors()),
			handler);
	}

	public static void main(String[] args) {
		System.setProperty("spring.main.lazy-initialization", "true");
		SpringApplication.run(Application.class, args);
	}
}
