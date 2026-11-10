package org.etf.pumpd.service;

import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import org.etf.pumpd.dto.PrijavaOdgovor;
import org.etf.pumpd.dto.PrijavaZahtjev;
import org.etf.pumpd.entity.Nalog;
import org.etf.pumpd.enums.Uloga;
import org.etf.pumpd.exception.NeispravnaPrijavaException;
import org.etf.pumpd.security.JwtPodaci;
import org.etf.pumpd.security.JwtService;

@Service
public class AuthService {

	private final KorisnikService korisnikService;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final NevazeciTokenService nevazeciTokenService;

	/**
	 * Hash nasumične lozinke; provjerava se kada email ne postoji, da bi odgovor
	 * trajao isto kao kod pogrešne lozinke i ne bi otkrivao koji emailovi postoje.
	 */
	private final String laznaLozinkaHash;

	public AuthService(KorisnikService korisnikService, PasswordEncoder passwordEncoder,
			JwtService jwtService, NevazeciTokenService nevazeciTokenService) {
		this.korisnikService = korisnikService;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.nevazeciTokenService = nevazeciTokenService;
		this.laznaLozinkaHash = passwordEncoder.encode("lazna-lozinka-za-ujednacavanje-vremena");
	}

	/** 1.1 Prijava: pogrešan email ili lozinka (ili nalog bez uloge) daju 401. */
	public PrijavaOdgovor prijava(PrijavaZahtjev zahtjev)
	{
		Optional<Nalog> nalog = korisnikService.pronadjiPoEmailu(zahtjev.email().trim());

		if (nalog.isEmpty()) {
			passwordEncoder.matches(zahtjev.password(), laznaLozinkaHash);
			throw new NeispravnaPrijavaException();
		}
		if (!passwordEncoder.matches(zahtjev.password(), nalog.get().getLozinka())) {
			throw new NeispravnaPrijavaException();
		}

		Nalog n = nalog.get();
		Uloga uloga = korisnikService.odrediUlogu(n.getId())
				.orElseThrow(NeispravnaPrijavaException::new);

		String token = jwtService.generisiToken(n.getId(), uloga);
		return new PrijavaOdgovor(n.getId(), n.getIme(), n.getPrezime(), n.getEmail(), uloga, token);
	}

	/** 1.2 Odjava: token ulazi u listu nevažećih do svog isteka. */
	public void odjava(JwtPodaci podaci)
	{
		nevazeciTokenService.ponisti(podaci.jti(), podaci.istice());
	}
}
