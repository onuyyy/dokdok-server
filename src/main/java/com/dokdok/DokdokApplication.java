package com.dokdok;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class DokdokApplication {

	private final Environment env;

	public DokdokApplication(Environment env) {
		this.env = env;
	}

	public static void main(String[] args) {
		SpringApplication.run(DokdokApplication.class, args);
	}

	@EventListener(ApplicationReadyEvent.class)
	public void onApplicationReady() {
		String profile = String.join(", ", env.getActiveProfiles());
		String port = env.getProperty("server.port", "8080");
		String dbUrl = env.getProperty("spring.datasource.url", "N/A");
		String javaVersion = System.getProperty("java.version");
		String jvmMemory = (Runtime.getRuntime().maxMemory() / 1024 / 1024) + "MB";

		System.out.println();
		System.out.println("======================================");
		System.out.println("   서버 실행 완료");
		System.out.println("======================================");
		System.out.println("  Profile  : " + profile);
		System.out.println("  Port     : " + port);
		System.out.println("  Java     : " + javaVersion);
		System.out.println("  JVM Heap : " + jvmMemory);
		System.out.println("  DB       : " + dbUrl);
		System.out.println("======================================");
		System.out.println();
	}
}
