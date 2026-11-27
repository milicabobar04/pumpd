package org.etf.pumpd.controller;

import java.util.List;

import org.etf.pumpd.dto.TreningIzIstorijeDto;
import org.etf.pumpd.security.TrenutniKorisnik;
import org.etf.pumpd.service.IstorijaTreningaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 2.4. Samo MEMBER (pravilo u SecurityConfig). */
@RestController
@RequestMapping("/api/users/training-history")
public class IstorijaTreningaController {

	private final IstorijaTreningaService istorijaService;
	private final TrenutniKorisnik trenutniKorisnik;

	public IstorijaTreningaController(IstorijaTreningaService istorijaService, TrenutniKorisnik trenutniKorisnik) {
		this.istorijaService = istorijaService;
		this.trenutniKorisnik = trenutniKorisnik;
	}

	@GetMapping
	public List<TreningIzIstorijeDto> istorija() {
		return istorijaService.istorija(trenutniKorisnik.obavezanId());
	}
}
