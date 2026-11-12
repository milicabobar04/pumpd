package org.etf.pumpd;

import java.time.ZoneId;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mysql.MySQLContainer;

/**
 * Prazna MySQL 8.4 baza u Dockeru za testove koji pokreću cijelu aplikaciju.
 * Spring je sam spaja umjesto DB_URL/DB_USER/DB_PASSWORD (@ServiceConnection),
 * a Flyway na njoj izvrši sve migracije i seed podatke (lozinka pumpd123).
 * Razvojna baza se ne dira. Testovi koji je koriste se preskaču ako Docker nije dostupan.
 */
@TestConfiguration(proxyBeanMethods = false)
public class MySqlTestKonfiguracija {

	@Bean
	@ServiceConnection
	MySQLContainer mysql() {
		return new MySQLContainer("mysql:8.4")
				// ista vremenska zona kao aplikacija: seed koristi CURDATE(), a servisi Clock
				.withEnv("TZ", ZoneId.systemDefault().getId())
				// trigeri iz V3 se prave bez SUPER privilegije, a binarni log je uključen
				.withCommand("--log-bin-trust-function-creators=1");
	}
}
