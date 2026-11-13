package org.etf.pumpd.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import org.etf.pumpd.dto.PorukaDto;
import org.etf.pumpd.dto.PromjenaLozinkeZahtjev;
import org.etf.pumpd.entity.Nalog;
import org.etf.pumpd.repository.NalogRepository;

/**
 * Promjena sopstvene lozinke (PUT /api/users/my-password), za sve prijavljene korisnike.
 * Pogrešna stara lozinka daje 400, a ne 401, da aplikacija ne bi odjavila korisnika.
 * Ostali uređaji se ne odjavljuju (poznato ograničenje, kao i nepostojanje ograničenja broja pokušaja).
 */
@Service
public class LozinkaService {

	private final NalogRepository nalogRepository;
	private final PasswordEncoder passwordEncoder;

	public LozinkaService(NalogRepository nalogRepository, PasswordEncoder passwordEncoder) {
		this.nalogRepository = nalogRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public PorukaDto promijeniLozinku(Integer idNalog, PromjenaLozinkeZahtjev zahtjev) {
		Nalog nalog = nalogRepository.findById(idNalog)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Nalog vise ne postoji."));

		if (!passwordEncoder.matches(zahtjev.oldPassword(), nalog.getLozinka())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stara lozinka nije ispravna.");
		}
		if (zahtjev.newPassword().equals(zahtjev.oldPassword())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nova lozinka mora biti razlicita od stare.");
		}

		nalog.promijeniLozinku(passwordEncoder.encode(zahtjev.newPassword()));
		nalogRepository.save(nalog);
		return new PorukaDto("Lozinka je promijenjena.");
	}
}
