package jp.ac.kinki_pc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AdminMainApplication {

	public static void main(String[] args) {
		SpringApplication.run(AdminMainApplication.class, args);
	}

}
