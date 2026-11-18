package org.etf.pumpd.controller;

import org.etf.pumpd.dto.*;
import org.etf.pumpd.security.*;
import org.etf.pumpd.service.*;
import org.springframework.http.HttpStatus;
import java.time.LocalDate;
import java.util.*;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/trainer")
public class TerminTrenerController {
	private final TerminTreneraService terminService;
	private final TrenutniKorisnik trenutniKorisnik;

	public TerminTrenerController(TerminTreneraService terminService, TrenutniKorisnik trenutniKorisnik) {
		this.terminService = terminService;
		this.trenutniKorisnik = trenutniKorisnik;
	}

	@GetMapping("/schedule")
	public List<TerminRasporedDto> raspored(
			@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate date)
	{
		return terminService.raspored(trenutniKorisnik.obavezanId(),date);
	}
	
	@PostMapping("/group-trainings")
	@ResponseStatus(HttpStatus.CREATED)
	public KreiranjeTerminaOdgovor kreiraj(@Valid @RequestBody TerminZahtjev zahtjev)
	{
		return terminService.kreiraj(trenutniKorisnik.obavezanId(),zahtjev);
	}

	@PutMapping("/group-trainings/{sessionId}")
	public PorukaDto izmijeni(@PathVariable Integer sessionId, @Valid @RequestBody TerminZahtjev zahtjev)
	{
		return terminService.izmijeni(trenutniKorisnik.obavezanId(),sessionId,zahtjev);
	}
	
	@DeleteMapping("/group-trainings/{sessionId}")
	public PorukaDto obrisi(@PathVariable Integer sessionId)
	{
		return terminService.obrisi(trenutniKorisnik.obavezanId(),sessionId);
	}
}
