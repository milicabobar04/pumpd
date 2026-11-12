package org.etf.pumpd.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.etf.pumpd.dto.ObradaZahtjevaZahtjev;
import org.etf.pumpd.dto.PorukaDto;
import org.etf.pumpd.dto.PratilacDto;
import org.etf.pumpd.dto.ZahtjevZaPracenjeDto;
import org.etf.pumpd.security.TrenutniKorisnik;
import org.etf.pumpd.service.PracenjeService;

import jakarta.validation.Valid;

/**
 * Rute 10.1–10.3: trenerova strana praćenja. Pristup (TRAINER) provjerava SecurityConfig,
 * a id trenera se uvijek uzima iz tokena.
 */
@RestController
@RequestMapping("/api/trainer")
public class TrenerPracenjeController {

	private final PracenjeService pracenjeService;
	private final TrenutniKorisnik trenutniKorisnik;

	public TrenerPracenjeController(PracenjeService pracenjeService, TrenutniKorisnik trenutniKorisnik) {
		this.pracenjeService = pracenjeService;
		this.trenutniKorisnik = trenutniKorisnik;
	}

	@GetMapping("/follow-requests")
	public List<ZahtjevZaPracenjeDto> zahtjevi() {
		return pracenjeService.zahtjeviZaTrenera(trenutniKorisnik.obavezanId());
	}

	@PatchMapping("/follow-requests/{requestId}")
	public PorukaDto obradiZahtjev(@PathVariable Integer requestId, @Valid @RequestBody ObradaZahtjevaZahtjev zahtjev) {
		return pracenjeService.obradiZahtjev(requestId, zahtjev.status(), trenutniKorisnik.obavezanId());
	}

	@GetMapping("/followers")
	public List<PratilacDto> pratioci() {
		return pracenjeService.pratioci(trenutniKorisnik.obavezanId());
	}
}
