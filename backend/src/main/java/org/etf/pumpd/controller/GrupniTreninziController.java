package org.etf.pumpd.controller;

import java.time.LocalDate;
import java.util.List;

import org.etf.pumpd.dto.PorukaDto;
import org.etf.pumpd.dto.TerminGrupnogTreningaDto;
import org.etf.pumpd.dto.ZahtjevZaPrijavuOdgovor;
import org.etf.pumpd.enums.Uloga;
import org.etf.pumpd.security.TrenutniKorisnik;
import org.etf.pumpd.service.GrupniTreninziService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Rute 9.x. Pregled (9.1) MEMBER i WORKER, prijava i odjava samo MEMBER (pravila u SecurityConfig). */
@RestController
@RequestMapping("/api/group-trainings")
public class GrupniTreninziController {

	private final GrupniTreninziService grupniTreninziService;
	private final TrenutniKorisnik trenutniKorisnik;

	public GrupniTreninziController(GrupniTreninziService grupniTreninziService, TrenutniKorisnik trenutniKorisnik) {
		this.grupniTreninziService = grupniTreninziService;
		this.trenutniKorisnik = trenutniKorisnik;
	}

	/** 9.1: enrollmentStatus samo za člana, pa se radniku id ne prosljeđuje. */
	@GetMapping
	public List<TerminGrupnogTreningaDto> pregled(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@RequestParam(required = false) Integer trainerId) {
		Integer idClana = trenutniKorisnik.obaveznaUloga() == Uloga.MEMBER ? trenutniKorisnik.obavezanId() : null;
		return grupniTreninziService.pregled(idClana, date, trainerId);
	}

	/** 9.2: tijelo zahtjeva je prazno. */
	@PostMapping("/{sessionId}/enrollment")
	@ResponseStatus(HttpStatus.CREATED)
	public ZahtjevZaPrijavuOdgovor posaljiZahtjev(@PathVariable Integer sessionId) {
		return grupniTreninziService.posaljiZahtjev(sessionId, trenutniKorisnik.obavezanId());
	}

	/** 9.3 */
	@DeleteMapping("/{sessionId}/enrollment")
	public PorukaDto odjava(@PathVariable Integer sessionId) {
		return grupniTreninziService.odjava(sessionId, trenutniKorisnik.obavezanId());
	}
}
