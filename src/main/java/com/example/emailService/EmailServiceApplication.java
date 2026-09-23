package com.example.emailService;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

/*
 * DataSourceAutoConfiguration and HibernateJpaAutoConfiguration were excluded
 * here. That was reasonable while email_db held no entities -- the datasource
 * was configured but unused, and excluding it let the service start without a
 * reachable MySQL.
 *
 * ProcessedEvent changes that. EmailConsumer cannot remember which eventIds it
 * has already sent without persistence, so the exclusions are lifted.
 *
 * OPERATIONAL CONSEQUENCE: email-service now needs its database at startup
 * where it previously did not. Both compose files already pass DB_URL and gate
 * it behind mysql's healthcheck, so this holds there -- but a deployment that
 * relied on it starting database-free will not.
 */
@EnableKafka
@SpringBootApplication
/* ProcessedEventPurge trims the dedup table nightly. */
@EnableScheduling
public class EmailServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(EmailServiceApplication.class, args);
	}

}
