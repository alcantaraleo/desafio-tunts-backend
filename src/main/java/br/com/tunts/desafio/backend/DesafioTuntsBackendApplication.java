package br.com.tunts.desafio.backend;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@Slf4j
public class DesafioTuntsBackendApplication {
	

	@PostConstruct
	public void onCreate() {
		log.info("### DesafioTuntsBackendApplication started ###");
	}
	
	@PreDestroy
	public void onDestroy() {
		log.info("### DesafioTuntsBackendApplication ended ###");
	}

	public static void main(String[] args) {
		SpringApplication.run(DesafioTuntsBackendApplication.class, args);
	}

}
