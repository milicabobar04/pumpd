package org.etf.pumpd.controller;

import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.etf.pumpd.dto.PorukaDto;
import org.etf.pumpd.dto.PromjenaLozinkeZahtjev;
import org.etf.pumpd.security.TrenutniKorisnik;
import org.etf.pumpd.service.LozinkaService;

import jakarta.validation.Valid;

/** Promjena sopstvene lozinke, za sve prijavljene korisnike (pokriva anyRequest().authenticated()). */
@RestController
@RequestMapping("/api/users/my-password")
public class LozinkaController {

	private final LozinkaService lozinkaService;
	private final TrenutniKorisnik trenutniKorisnik;

	public LozinkaController(LozinkaService lozinkaService, TrenutniKorisnik trenutniKorisnik) {
		this.lozinkaService = lozinkaService;
		this.trenutniKorisnik = trenutniKorisnik;
	}

	@PutMapping
	public PorukaDto promijeniLozinku(@Valid @RequestBody PromjenaLozinkeZahtjev zahtjev) {
		return lozinkaService.promijeniLozinku(trenutniKorisnik.obavezanId(), zahtjev);
	}
}
