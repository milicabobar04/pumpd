package org.etf.pumpd;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Aplikacija se pokreće nad praznom MySQL bazom u Dockeru (migracije V1–V6 prolaze). */
@SpringBootTest
@Import(MySqlTestKonfiguracija.class)
@Testcontainers(disabledWithoutDocker = true)
class BackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
