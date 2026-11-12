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
import org.etf.pumpd.dto.DodavanjeRadnikaOdgovor;
import org.etf.pumpd.dto.DodavanjeRadnikaZahtjev;
import org.etf.pumpd.dto.RadnikDto;
import org.etf.pumpd.service.AdminRadniciService;

import jakarta.validation.Valid;

/** Rute 12.5–12.8 za administratora. Pristup (ADMIN) provjerava SecurityConfig. */
@RestController
@RequestMapping("/api/admin/workers")
public class AdminRadniciController {

	private final AdminRadniciService adminRadniciService;

	public AdminRadniciController(AdminRadniciService adminRadniciService) {
		this.adminRadniciService = adminRadniciService;
	}

	@GetMapping
	public List<RadnikDto> listaRadnika() {
		return adminRadniciService.listaRadnika();
	}

	@GetMapping("/{workerId}")
	public RadnikDto detaljiRadnika(@PathVariable Integer workerId) {
		return adminRadniciService.detaljiRadnika(workerId);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public DodavanjeRadnikaOdgovor dodajRadnika(@Valid @RequestBody DodavanjeRadnikaZahtjev zahtjev) {
		return adminRadniciService.dodajRadnika(zahtjev);
	}

	@DeleteMapping("/{workerId}")
	public PorukaDto ukloniRadnika(@PathVariable Integer workerId) {
		return adminRadniciService.ukloniRadnika(workerId);
	}
}
