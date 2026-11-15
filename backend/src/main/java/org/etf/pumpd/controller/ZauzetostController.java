package org.etf.pumpd.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.etf.pumpd.dto.ZauzetostDto;
import org.etf.pumpd.service.DolazakService;

/**
 * 5.3: zauzetost teretane, za svakog prijavljenog korisnika (anyRequest().authenticated()).
 * Odvojen od TeretanaController jer koristi evidenciju dolazaka, ne informacije o teretani.
 */
@RestController
@RequestMapping("/api/gym")
public class ZauzetostController {

	private final DolazakService dolazakService;

	public ZauzetostController(DolazakService dolazakService) {
		this.dolazakService = dolazakService;
	}

	@GetMapping("/occupancy")
	public ZauzetostDto zauzetost() {
		return dolazakService.zauzetost();
	}
}
