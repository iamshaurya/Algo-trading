package com.shaurya.intraday;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@ComponentScan("com.shaurya")
@SpringBootApplication
//@EnableScheduling
public class ShauryaIntradayApplication extends SpringBootServletInitializer{

	public static void main(String[] args) {
		SpringApplication.run(ShauryaIntradayApplication.class, args);
	}
	
	@Override
	protected SpringApplicationBuilder configure(final SpringApplicationBuilder application) {
		return application.sources(ShauryaIntradayApplication.class);
	}
}
