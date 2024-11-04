package com.bisoft.bfm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import com.bisoft.bfm.model.BfmContext;

@SpringBootApplication
@EnableScheduling
public class BfmApplication implements CommandLineRunner {
	@Autowired
	BfmContext bfmContext;

	@Bean(destroyMethod = "shutdown")
	public ThreadPoolTaskScheduler taskScheduler() {
		ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.setPoolSize(10);
		return  taskScheduler;
	}

	@Bean
	public static PropertySourcesPlaceholderConfigurer createPropertyConfigurer()
	{
		PropertySourcesPlaceholderConfigurer propertyConfigurer = new PropertySourcesPlaceholderConfigurer();
		propertyConfigurer.setTrimValues(true);
		return propertyConfigurer;
	}


	public static void main(String[] args) {
		SpringApplication.run(BfmApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {

	}
}
