package com.forum;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ForumApplication {

	public static void main(String[] args) {
		// La app trabaja en UTC (todos los timestamps son Instant). Además evita que el
		// driver de Postgres propague zonas horarias legacy de la JVM que el motor rechaza.
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
		SpringApplication.run(ForumApplication.class, args);
	}

}
