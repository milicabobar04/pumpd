package org.etf.pumpd.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import org.etf.pumpd.entity.Nalog;
import org.etf.pumpd.exception.KonfliktException;
import org.etf.pumpd.repository.NalogRepository;

/**
 * Zajednički dio dodavanja naloga za 11.3 (član), 12.3 (trener) i 12.7 (radnik).
 * Nema svoj @Transactional: radi u transakciji servisa koji ga poziva, pa se nalog
 * poništava ako upis člana, trenera ili radnika ne uspije.
 */
@Service
public class NoviNalogService {

	static final String EMAIL_POSTOJI = "Email vec postoji.";

	private final NalogRepository nalogRepository;
	private final PasswordEncoder passwordEncoder;
	private final GeneratorLozinke generatorLozinke;

	public NoviNalogService(NalogRepository nalogRepository, PasswordEncoder passwordEncoder,
			GeneratorLozinke generatorLozinke) {
		this.nalogRepository = nalogRepository;
		this.passwordEncoder = passwordEncoder;
		this.generatorLozinke = generatorLozinke;
	}

	/**
	 * Upisani nalog i njegova privremena lozinka u čitljivom obliku. Lozinka postoji samo ovdje
	 * (u bazi je heš) i vraća se jednom, u odgovoru rute za dodavanje.
	 */
	public record NoviNalog(Nalog nalog, String privremenaLozinka) {
	}

	/**
	 * Čisti razmake, provjerava da email nije zauzet (409), generiše privremenu lozinku,
	 * hešira je i odmah upisuje nalog sa oznakom da lozinku treba promijeniti.
	 * Email se ne pretvara u mala slova: MySQL ga poredi bez obzira na velika i mala slova.
	 */
	public NoviNalog kreirajNalog(String ime, String prezime, String email) {
		String ocisceniEmail = email.trim();
		if (nalogRepository.existsByEmail(ocisceniEmail)) {
			throw new KonfliktException(EMAIL_POSTOJI);
		}

		String privremenaLozinka = generatorLozinke.generisi();
		Nalog nalog = new Nalog(ime.trim(), prezime.trim(), ocisceniEmail, null);
		nalog.postaviPrivremenuLozinku(passwordEncoder.encode(privremenaLozinka));
		try {
			// saveAndFlush odmah šalje INSERT, pa UNIQUE indeks na emailu (istovremeni upis) puca ovdje
			return new NoviNalog(nalogRepository.saveAndFlush(nalog), privremenaLozinka);
		} catch (DataIntegrityViolationException e) {
			throw new KonfliktException(EMAIL_POSTOJI);
		}
	}
}
