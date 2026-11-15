package org.etf.pumpd.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.etf.pumpd.dto.EvidencijaDolaskaZahtjev;
import org.etf.pumpd.dto.IzlazakOdgovor;
import org.etf.pumpd.dto.UlazakOdgovor;
import org.etf.pumpd.service.DolazakService;

import jakarta.validation.Valid;

/** 11.8, 11.9. Samo WORKER (pravilo /api/worker/attendance/** u SecurityConfig). */
@RestController
@RequestMapping("/api/worker/attendance")
public class DolazakController {

	private final DolazakService dolazakService;

	public DolazakController(DolazakService dolazakService) {
		this.dolazakService = dolazakService;
	}

	@PostMapping("/entry")
	public UlazakOdgovor ulazak(@Valid @RequestBody EvidencijaDolaskaZahtjev zahtjev) {
		return dolazakService.ulazak(zahtjev.qrCode());
	}

	@PostMapping("/exit")
	public IzlazakOdgovor izlazak(@Valid @RequestBody EvidencijaDolaskaZahtjev zahtjev) {
		return dolazakService.izlazak(zahtjev.qrCode());
	}
}
