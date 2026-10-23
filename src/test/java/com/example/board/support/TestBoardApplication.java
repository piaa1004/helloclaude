package com.example.board.support;

import com.example.board.BoardApplication;
import org.springframework.boot.SpringApplication;

// docker compose 없이 Testcontainers DB로 앱을 띄울 때 사용한다 (IntelliJ에서 실행 또는 ./gradlew bootTestRun).
public class TestBoardApplication {

	public static void main(String[] args) {
		SpringApplication.from(BoardApplication::main)
				.with(TestcontainersConfiguration.class)
				.withAdditionalProfiles("test")
				.run(args);
	}
}
