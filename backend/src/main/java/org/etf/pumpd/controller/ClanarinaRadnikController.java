package org.etf.pumpd.controller;

import org.etf.pumpd.dto.ClanarinaDto;
import org.etf.pumpd.dto.IzmjenaClanarineZahtjev;
import org.etf.pumpd.dto.PorukaDto;
import org.etf.pumpd.dto.ProduzenjeClanarineOdgovor;
import org.etf.pumpd.dto.ProduzenjeClanarineZahtjev;
import org.etf.pumpd.service.ClanarinaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/worker/members/{memberId}/membership")
public class ClanarinaRadnikController {
	



	public ClanarinaRadnikController(ClanarinaService clanarinaService) {
		super();
		this.clanarinaService = clanarinaService;
	}

	private final ClanarinaService clanarinaService;


	

	@GetMapping
	public ClanarinaDto clanarina(@PathVariable Integer memberId) {
		return clanarinaService.clanarinaClana(memberId);
	}
	
	@PutMapping
	public PorukaDto postavi(@PathVariable Integer memberId, @Valid @RequestBody IzmjenaClanarineZahtjev zahtjev) {
		return clanarinaService.postaviClanarinu(memberId,zahtjev);
	}
	
	@PostMapping("/extend")
	public ProduzenjeClanarineOdgovor produzi(@PathVariable Integer memberId, @Valid @RequestBody ProduzenjeClanarineZahtjev zahtjev) {
		return clanarinaService.produziClanarinu(memberId,zahtjev);
	}


}
