package org.etf.pumpd.controller;

import java.util.List;

import org.etf.pumpd.dto.ObradaZahtjevaZahtjev;
import org.etf.pumpd.dto.PorukaDto;
import org.etf.pumpd.dto.ZahtjevZaGrupniTreningDto;
import org.etf.pumpd.security.TrenutniKorisnik;
import org.etf.pumpd.service.ZahtjeviZaGrupeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * 10.12, 10.13. Samo TRAINER (pravilo /api/trainer/** u SecurityConfig). Tijelo 10.13 ima isti oblik
 * kao 10.2 ({"status": "ACCEPTED" | "REJECTED"}), pa se koristi isti DTO.
 */
@RestController
@RequestMapping("/api/trainer/group-requests")
public class ZahtjeviZaGrupeController {

	private final ZahtjeviZaGrupeService zahtjeviService;
	private final TrenutniKorisnik trenutniKorisnik;

	public ZahtjeviZaGrupeController(ZahtjeviZaGrupeService zahtjeviService, TrenutniKorisnik trenutniKorisnik) {
		this.zahtjeviService = zahtjeviService;
		this.trenutniKorisnik = trenutniKorisnik;
	}

	@GetMapping
	public List<ZahtjevZaGrupniTreningDto> zahtjevi(@RequestParam(required = false) Integer sessionId) {
		return zahtjeviService.zahtjevi(trenutniKorisnik.obavezanId(), sessionId);
	}

	@PatchMapping("/{requestId}")
	public PorukaDto obradi(@PathVariable Integer requestId, @Valid @RequestBody ObradaZahtjevaZahtjev zahtjev) {
		return zahtjeviService.obradi(trenutniKorisnik.obavezanId(), requestId, zahtjev.status());
	}
}
