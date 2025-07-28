package com.groupe.gestin_inscription;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class GestinInscriptionApplication {

	public static void main(String[] args) {
		SpringApplication.run(GestinInscriptionApplication.class, args);
	}

}
