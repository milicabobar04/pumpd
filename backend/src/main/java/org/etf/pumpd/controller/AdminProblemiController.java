package org.etf.pumpd.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.etf.pumpd.dto.ProblemDto;
import org.etf.pumpd.dto.PromjenaStatusaProblemaOdgovor;
import org.etf.pumpd.dto.PromjenaStatusaProblemaZahtjev;
import org.etf.pumpd.enums.StatusProblema;
import org.etf.pumpd.security.TrenutniKorisnik;
import org.etf.pumpd.service.ProblemService;

import jakarta.validation.Valid;

/** Rute 4.2 i 4.3 za administratora. Pristup (ADMIN) provjerava SecurityConfig. */
@RestController
@RequestMapping("/api/admin/issues")
public class AdminProblemiController {

	private final ProblemService problemService;
	private final TrenutniKorisnik trenutniKorisnik;

	public AdminProblemiController(ProblemService problemService, TrenutniKorisnik trenutniKorisnik) {
		this.problemService = problemService;
		this.trenutniKorisnik = trenutniKorisnik;
	}

	@GetMapping
	public List<ProblemDto> listaProblema(@RequestParam(required = false) StatusProblema status) {
		return problemService.listaProblema(status);
	}

	@PatchMapping("/{issueId}")
	public PromjenaStatusaProblemaOdgovor promijeniStatus(@PathVariable Integer issueId,
			@Valid @RequestBody PromjenaStatusaProblemaZahtjev zahtjev) {
		return problemService.promijeniStatus(issueId, zahtjev.status(), trenutniKorisnik.obavezanId());
	}
}
