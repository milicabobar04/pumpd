package org.etf.pumpd.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import org.etf.pumpd.dto.PorukaDto;
import org.etf.pumpd.dto.RecenzijaOdgovor;
import org.etf.pumpd.dto.RecenzijaZahtjev;
import org.etf.pumpd.dto.TrenerZaClanaDetaljiDto;
import org.etf.pumpd.dto.TrenerZaClanaDto;
import org.etf.pumpd.dto.ZahtjevZaPracenjeOdgovor;
import org.etf.pumpd.security.TrenutniKorisnik;
import org.etf.pumpd.service.PracenjeService;
import org.etf.pumpd.service.RecenzijeService;
import org.etf.pumpd.service.TreneriService;

import jakarta.validation.Valid;

/** Rute 7.1–7.5 za člana. Pristup (MEMBER) provjerava SecurityConfig. */
@RestController
@RequestMapping("/api/trainers")
public class TreneriController {

	private final TreneriService treneriService;
	private final PracenjeService pracenjeService;
	private final RecenzijeService recenzijeService;
	private final TrenutniKorisnik trenutniKorisnik;

	public TreneriController(TreneriService treneriService, PracenjeService pracenjeService,
			RecenzijeService recenzijeService, TrenutniKorisnik trenutniKorisnik) {
		this.treneriService = treneriService;
		this.pracenjeService = pracenjeService;
		this.recenzijeService = recenzijeService;
		this.trenutniKorisnik = trenutniKorisnik;
	}

	@GetMapping
	public List<TrenerZaClanaDto> listaTrenera() {
		return treneriService.listaTrenera();
	}

	@GetMapping("/{trainerId}")
	public TrenerZaClanaDetaljiDto detaljiTrenera(@PathVariable Integer trainerId) {
		return treneriService.detaljiTrenera(trainerId, trenutniKorisnik.obavezanId());
	}

	@PostMapping("/{trainerId}/follow")
	@ResponseStatus(HttpStatus.CREATED)
	public ZahtjevZaPracenjeOdgovor posaljiZahtjev(@PathVariable Integer trainerId) {
		return pracenjeService.posaljiZahtjev(trainerId, trenutniKorisnik.obavezanId());
	}

	@DeleteMapping("/{trainerId}/follow")
	public PorukaDto prekiniPracenje(@PathVariable Integer trainerId) {
		return pracenjeService.prekini(trainerId, trenutniKorisnik.obavezanId());
	}

	@PostMapping("/{trainerId}/reviews")
	@ResponseStatus(HttpStatus.CREATED)
	public RecenzijaOdgovor ostaviRecenziju(@PathVariable Integer trainerId,
			@Valid @RequestBody RecenzijaZahtjev zahtjev) {
		return recenzijeService.ostaviRecenziju(trainerId, trenutniKorisnik.obavezanId(), zahtjev);
	}
}
