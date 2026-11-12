package org.etf.pumpd.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.etf.pumpd.dto.RecenzijeTreneraDto;
import org.etf.pumpd.security.TrenutniKorisnik;
import org.etf.pumpd.service.RecenzijeService;

/** Ruta 7.6: trener pregleda svoje recenzije. Pristup (TRAINER) provjerava SecurityConfig. */
@RestController
@RequestMapping("/api/trainer/reviews")
public class TrenerRecenzijeController {

	private final RecenzijeService recenzijeService;
	private final TrenutniKorisnik trenutniKorisnik;

	public TrenerRecenzijeController(RecenzijeService recenzijeService, TrenutniKorisnik trenutniKorisnik) {
		this.recenzijeService = recenzijeService;
		this.trenutniKorisnik = trenutniKorisnik;
	}

	@GetMapping
	public RecenzijeTreneraDto mojeRecenzije() {
		return recenzijeService.recenzijeTrenera(trenutniKorisnik.obavezanId());
	}
}
