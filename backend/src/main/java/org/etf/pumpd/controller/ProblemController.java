package org.etf.pumpd.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.*;
import org.etf.pumpd.dto.*;
import org.etf.pumpd.security.*;
import org.etf.pumpd.service.*;

@RestController
@RequestMapping("/api/issues")
public class ProblemController {

	public ProblemController(ProblemService problemService, TrenutniKorisnik trenutniKorisnik) {
		super();
		this.problemService = problemService;
		this.trenutniKorisnik = trenutniKorisnik;
	}

	private final ProblemService problemService;
	private final TrenutniKorisnik trenutniKorisnik;
	
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public PrijavaProblemaOdgovor prijaviProblem(@RequestBody PrijavaProblemaZahtjev zahtjev)
	{
		return problemService.prijavi(trenutniKorisnik.id().orElse(null),zahtjev);
	}
}
