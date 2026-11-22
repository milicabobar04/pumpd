package org.etf.pumpd.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.etf.pumpd.dto.*;
import org.etf.pumpd.security.*;
import org.etf.pumpd.service.*;
import java.util.*;


@RestController
@RequestMapping("/api/notifications")
public class ObavjestenjeController {

	public ObavjestenjeController(ObavjestenjeService obavjestenjeService, TrenutniKorisnik trenutniKorisnik) {
		super();
		this.obavjestenjeService = obavjestenjeService;
		this.trenutniKorisnik = trenutniKorisnik;
	}

	private final ObavjestenjeService obavjestenjeService;
	private final TrenutniKorisnik trenutniKorisnik;
	
	@GetMapping
	public List<ObavjestenjeDto> mojaObavjestenja()
	{
		return obavjestenjeService.mojaObavjestenja(trenutniKorisnik.obavezanId());
	}

	/** Označava sva obavještenja prijavljenog korisnika kao pročitana. Tijelo zahtjeva je prazno. */
	@PatchMapping("/read-all")
	public PorukaDto oznaciSveKaoProcitana()
	{
		obavjestenjeService.oznaciSveKaoProcitana(trenutniKorisnik.obavezanId());
		return new PorukaDto("Obavjestenja su oznacena kao procitana.");
	}

	/** Označava jedno obavještenje prijavljenog korisnika kao pročitano. Tijelo zahtjeva je prazno. */
	@PatchMapping("/{notificationId}/read")
	public PorukaDto oznaciKaoProcitano(@PathVariable Integer notificationId)
	{
		obavjestenjeService.oznaciKaoProcitano(notificationId, trenutniKorisnik.obavezanId());
		return new PorukaDto("Obavjestenje je oznaceno kao procitano.");
	}
}
