package com.forum;

import org.springframework.boot.SpringApplication;

public class TestForumApplication {

	public static void main(String[] args) {
		SpringApplication.from(ForumApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
