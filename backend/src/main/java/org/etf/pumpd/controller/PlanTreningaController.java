package org.etf.pumpd.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.etf.pumpd.dto.*;
import org.etf.pumpd.security.TrenutniKorisnik;
import org.etf.pumpd.service.PlanTreningaService;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;


@RestController
@RequestMapping("/api/training-plans")
public class PlanTreningaController {

	private final PlanTreningaService planService;
	private final TrenutniKorisnik trenutniKorisnik;
	
	public PlanTreningaController(PlanTreningaService planService, TrenutniKorisnik trenutniKorisnik) {
		this.planService = planService;
		this.trenutniKorisnik = trenutniKorisnik;
	}
	
	@GetMapping
	public List<PlanListDto> listaPlanova() {
		return planService.listaPlanova(trenutniKorisnik.obavezanId());
	}

	@GetMapping("/{planId}")
	public PlanDetaljiDto detaljiPlana(@PathVariable Integer planId) {
		return planService.detaljiPlana(trenutniKorisnik.obavezanId(), planId);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public KreiranjePlanaOdgovor kreirajPlan(@Valid @RequestBody PlanZahtjev zahtjev) {
		return planService.kreirajPlan(trenutniKorisnik.obavezanId(), trenutniKorisnik.obaveznaUloga(), zahtjev);
	}

	@PutMapping("/{planId}")
	public PorukaDto izmijeniPlan(@PathVariable Integer planId, @Valid @RequestBody PlanZahtjev zahtjev) {
		return planService.izmijeniPlan(trenutniKorisnik.obavezanId(), planId, zahtjev);
	}

	@DeleteMapping("/{planId}")
	public PorukaDto obrisiPlan(@PathVariable Integer planId) {
		return planService.obrisiPlan(trenutniKorisnik.obavezanId(), planId);
	}
}
