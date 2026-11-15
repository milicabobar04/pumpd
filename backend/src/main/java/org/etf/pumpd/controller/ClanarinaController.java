package org.etf.pumpd.controller;


import org.etf.pumpd.dto.ClanarinaClanaDto;
import org.etf.pumpd.security.TrenutniKorisnik;
import org.etf.pumpd.service.ClanarinaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/membership")
public class ClanarinaController {

	public ClanarinaController(ClanarinaService clanarinaService, TrenutniKorisnik trenutniKorisnik) {
		super();
		this.clanarinaService = clanarinaService;
		this.trenutniKorisnik = trenutniKorisnik;
	}



	private final ClanarinaService clanarinaService;
	private final TrenutniKorisnik trenutniKorisnik;

	

	@GetMapping("/my-status")
	public ClanarinaClanaDto mojaClanarina() {
		return clanarinaService.mojaClanarina(trenutniKorisnik.obavezanId());
	}

	
}
