package com.bisoft.bfm;

import java.util.Properties;

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
		taskScheduler.setPoolSize(20);
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
		// SpringApplication.run(BfmApplication.class, args);
		SpringApplication bfm = new SpringApplication(BfmApplication.class);
        Properties properties = new Properties();
        properties.put("management.endpoint.restart.enabled", Boolean.TRUE);
		properties.put("management.endpoints.web.exposure.include", "restart");
		properties.put("logging.pattern.console","%d %-22.22logger{0} : %m%n%wEx");
		properties.put("logging.file.name","log/app.log");
		properties.put("logging.pattern.file","%d : %m%n%wEx");
		properties.put("logging.file.max-size","10MB");
		properties.put("logging.file.max-history","5");
		properties.put("logging.file.total-size-cap","5GB");
		properties.put("spring.profiles.active", "@spring.profiles.active@");
        String customLogo = System.getProperty("app.custom-logo", "");
        properties.put("spring.web.resources.static-locations","classpath:/static/,file:" + customLogo);

        bfm.setDefaultProperties(properties);
		bfm.run(args);
	}

	@Override
	public void run(String... args) throws Exception {

	}
}
