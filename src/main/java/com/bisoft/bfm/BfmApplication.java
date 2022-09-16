package com.bisoft.bfm;

import com.bisoft.bfm.model.BfmContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.ArrayList;

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


	public static void main(String[] args) {
		SpringApplication.run(BfmApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {

	}
}
