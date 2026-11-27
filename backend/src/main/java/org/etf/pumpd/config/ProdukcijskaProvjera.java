package org.etf.pumpd.config;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.core.env.PropertyResolver;

/**
 * Sigurnost (2.4.3): u profilu "prod" (server za predaju) server se ne pokreće dok nisu postavljene
 * obavezne vrijednosti. Lokalno (profil "dev") i u testovima važe podrazumijevane vrijednosti iz
 * application.properties, a ključ JWT_SECRET odatle je javan (u gitu), pa u produkciji nije dozvoljen.
 *
 * Provjera radi u BeanFactoryPostProcessor-u, prije pravljenja ostalih komponenti, da poruka bude
 * jasna (umjesto npr. "jdbcUrl is required" iz baze ili greške o slabom ključu iz JwtService).
 */
@Configuration(proxyBeanMethods = false)
public class ProdukcijskaProvjera {

	/** Podrazumijevani ključ iz application.properties (javan, samo za razvoj i testove). */
	static final String RAZVOJNI_JWT_KLJUC = "CgQjUrqARTGnaq7/JWnCUsnbfPkUn66hTYqmmnGnzHQ=";

	@Bean
	static BeanFactoryPostProcessor provjeraProdukcijskihPostavki(Environment okruzenje) {
		return fabrika -> {
			if (okruzenje.acceptsProfiles(Profiles.of("prod"))) {
				provjeri(okruzenje);
			}
		};
	}

	/** Baca IllegalStateException sa svim propustima odjednom, da se ne popravljaju jedan po jedan. */
	static void provjeri(PropertyResolver postavke) {
		List<String> greske = new ArrayList<>();

		String jwt = postavke.getProperty("jwt.secret", "").trim();
		if (jwt.isEmpty()) {
			greske.add("JWT_SECRET mora biti postavljen u produkciji.");
		} else if (jwt.equals(RAZVOJNI_JWT_KLJUC)) {
			greske.add("JWT_SECRET ne smije biti razvojni ključ iz application.properties (javan je u gitu).");
		} else {
			try {
				if (Base64.getDecoder().decode(jwt).length < 32) {
					greske.add("JWT_SECRET mora imati najmanje 256 bita (32 bajta prije Base64 zapisa).");
				}
			} catch (IllegalArgumentException e) {
				greske.add("JWT_SECRET mora biti Base64 zapis (npr. openssl rand -base64 32).");
			}
		}

		if (postavke.getProperty("spring.datasource.url", "").isBlank()) {
			greske.add("DB_URL mora biti postavljen u produkciji.");
		}
		if (postavke.getProperty("spring.datasource.username", "").isBlank()) {
			greske.add("DB_USER mora biti postavljen u produkciji.");
		}
		if (postavke.getProperty("spring.datasource.password", "").isBlank()) {
			greske.add("DB_PASSWORD mora biti postavljen u produkciji.");
		}

		String slike = postavke.getProperty("pumpd.slike.folder", "").trim();
		if (slike.isEmpty()) {
			greske.add("SLIKE_FOLDER mora biti postavljen u produkciji (trajni folder za slike objava).");
		} else if (!Path.of(slike).isAbsolute()) {
			greske.add("SLIKE_FOLDER mora biti apsolutna putanja (npr. /var/pumpd/slike), da slike ne zavise od foldera iz kog je server pokrenut.");
		}

		if (!greske.isEmpty()) {
			throw new IllegalStateException("Server nije pokrenut, jer produkcijske postavke nisu potpune:\n - "
					+ String.join("\n - ", greske));
		}
	}
}
