package org.etf.pumpd.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import org.etf.pumpd.dto.ClanDetaljiDto;
import org.etf.pumpd.dto.ClanListaDto;
import org.etf.pumpd.dto.DodavanjeClanaOdgovor;
import org.etf.pumpd.dto.PorukaDto;
import org.etf.pumpd.dto.DodavanjeClanaZahtjev;
import org.etf.pumpd.service.ClanoviService;

import jakarta.validation.Valid;

/** Rute 11.x za zaposlene. Pristup (WORKER, ADMIN) provjerava SecurityConfig. */
@RestController
@RequestMapping("/api/worker/members")
public class ClanoviController {

	private final ClanoviService clanoviService;

	public ClanoviController(ClanoviService clanoviService) {
		this.clanoviService = clanoviService;
	}

	@GetMapping
	public List<ClanListaDto> listaClanova(@RequestParam(required = false) String search) {
		return clanoviService.listaClanova(search);
	}

	@GetMapping("/{memberId}")
	public ClanDetaljiDto detaljiClana(@PathVariable Integer memberId) {
		return clanoviService.detaljiClana(memberId);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public DodavanjeClanaOdgovor dodajClana(@Valid @RequestBody DodavanjeClanaZahtjev zahtjev) {
		return clanoviService.dodajClana(zahtjev);
	}

	@DeleteMapping("/{memberId}")
	public PorukaDto ukloniClana(@PathVariable Integer memberId) {
		return clanoviService.ukloniClana(memberId);
	}
}
