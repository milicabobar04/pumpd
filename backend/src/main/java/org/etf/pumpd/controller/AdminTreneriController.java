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
import org.etf.pumpd.dto.DodavanjeTreneraOdgovor;
import org.etf.pumpd.dto.DodavanjeTreneraZahtjev;
import org.etf.pumpd.dto.TrenerDetaljiDto;
import org.etf.pumpd.dto.TrenerListaDto;
import org.etf.pumpd.service.AdminTreneriService;

import jakarta.validation.Valid;

/** Rute 12.1–12.4 za administratora. Pristup (ADMIN) provjerava SecurityConfig. */
@RestController
@RequestMapping("/api/admin/trainers")
public class AdminTreneriController {

	private final AdminTreneriService adminTreneriService;

	public AdminTreneriController(AdminTreneriService adminTreneriService) {
		this.adminTreneriService = adminTreneriService;
	}

	@GetMapping
	public List<TrenerListaDto> listaTrenera() {
		return adminTreneriService.listaTrenera();
	}

	@GetMapping("/{trainerId}")
	public TrenerDetaljiDto detaljiTrenera(@PathVariable Integer trainerId) {
		return adminTreneriService.detaljiTrenera(trainerId);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public DodavanjeTreneraOdgovor dodajTrenera(@Valid @RequestBody DodavanjeTreneraZahtjev zahtjev) {
		return adminTreneriService.dodajTrenera(zahtjev);
	}

	@DeleteMapping("/{trainerId}")
	public PorukaDto ukloniTrenera(@PathVariable Integer trainerId) {
		return adminTreneriService.ukloniTrenera(trainerId);
	}
}
