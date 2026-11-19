package org.etf.pumpd.controller;

import org.etf.pumpd.dto.*;
import org.etf.pumpd.security.TrenutniKorisnik;
import org.etf.pumpd.service.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.*;

@RestController
@RequestMapping("/api/trainer/groups")
public class GrupaTreneraController {

	public GrupaTreneraController(GrupaTreneraService grupaService, TrenutniKorisnik trenutniKorisnik) {
		super();
		this.grupaService = grupaService;
		this.trenutniKorisnik = trenutniKorisnik;
	}

	private final GrupaTreneraService grupaService;
	private final TrenutniKorisnik trenutniKorisnik;
	
	@GetMapping
	public List<GrupaDto> listaGrupa()
	{
		return grupaService.listaGrupa(trenutniKorisnik.obavezanId());
	}
	
	@GetMapping("/{groupId}/members")
	public List<ClanGrupeDto> clanoviGrupe(@PathVariable Integer groupId)
	{
		return grupaService.clanoviGrupe(trenutniKorisnik.obavezanId(),groupId);
	}
	
	@PostMapping("/{groupId}/members")
	@ResponseStatus(HttpStatus.CREATED)
	public PorukaDto dodajClana(@PathVariable Integer groupId, @Valid @RequestBody DodavanjeUGrupuZahtjev zahtjev)
	{
		return grupaService.dodajClana(trenutniKorisnik.obavezanId(),groupId, zahtjev.memberId());
	}
	
	@DeleteMapping("/{groupId}/members/{memberId}")
	public PorukaDto ukloniClana(@PathVariable Integer groupId, @PathVariable Integer memberId)
	{
		return grupaService.ukloniClana(trenutniKorisnik.obavezanId(),groupId,memberId);
	}
}
