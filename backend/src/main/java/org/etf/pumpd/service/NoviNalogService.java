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

	public NoviNalogService(NalogRepository nalogRepository, PasswordEncoder passwordEncoder) {
		this.nalogRepository = nalogRepository;
		this.passwordEncoder = passwordEncoder;
	}

	/**
	 * Čisti razmake, provjerava da email nije zauzet (409), hešira lozinku i odmah upisuje nalog.
	 * Email se ne pretvara u mala slova: MySQL ga poredi bez obzira na velika i mala slova.
	 */
	public Nalog kreirajNalog(String ime, String prezime, String email, String lozinka) {
		String ocisceniEmail = email.trim();
		if (nalogRepository.existsByEmail(ocisceniEmail)) {
			throw new KonfliktException(EMAIL_POSTOJI);
		}

		Nalog nalog = new Nalog(ime.trim(), prezime.trim(), ocisceniEmail, passwordEncoder.encode(lozinka));
		try {
			// saveAndFlush odmah šalje INSERT, pa UNIQUE indeks na emailu (istovremeni upis) puca ovdje
			return nalogRepository.saveAndFlush(nalog);
		} catch (DataIntegrityViolationException e) {
			throw new KonfliktException(EMAIL_POSTOJI);
		}
	}
}
