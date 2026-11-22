package org.etf.pumpd.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import org.etf.pumpd.dto.MojaObjavaDto;
import org.etf.pumpd.dto.NovaObjavaOdgovor;
import org.etf.pumpd.dto.NovaObjavaZahtjev;
import org.etf.pumpd.dto.ObjavaDto;
import org.etf.pumpd.dto.PorukaDto;
import org.etf.pumpd.security.TrenutniKorisnik;
import org.etf.pumpd.service.ObjaveService;

import jakarta.validation.Valid;

/**
 * Rute 8.x. Uloge su pomiješane (trener: 8.5–8.7, član: 8.1–8.4), a pristup po metodi i
 * putanji provjerava SecurityConfig. Id korisnika se uzima iz tokena.
 */
@RestController
@RequestMapping("/api/posts")
public class ObjaveController {

	private final ObjaveService objaveService;
	private final TrenutniKorisnik trenutniKorisnik;

	public ObjaveController(ObjaveService objaveService, TrenutniKorisnik trenutniKorisnik) {
		this.objaveService = objaveService;
		this.trenutniKorisnik = trenutniKorisnik;
	}

	@GetMapping("/feed")
	public List<ObjavaDto> feed() {
		return objaveService.feed(trenutniKorisnik.obavezanId());
	}

	@PostMapping("/{postId}/save")
	public PorukaDto sacuvaj(@PathVariable Integer postId) {
		return objaveService.sacuvaj(postId, trenutniKorisnik.obavezanId());
	}

	@GetMapping("/saved")
	public List<ObjavaDto> sacuvane() {
		return objaveService.sacuvane(trenutniKorisnik.obavezanId());
	}

	@DeleteMapping("/{postId}/save")
	public PorukaDto ukloniIzSacuvanih(@PathVariable Integer postId) {
		return objaveService.ukloniIzSacuvanih(postId, trenutniKorisnik.obavezanId());
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public NovaObjavaOdgovor dodajObjavu(@Valid @RequestBody NovaObjavaZahtjev zahtjev) {
		return objaveService.dodajObjavu(trenutniKorisnik.obavezanId(), zahtjev);
	}

	@GetMapping("/my-posts")
	public List<MojaObjavaDto> mojeObjave() {
		return objaveService.mojeObjave(trenutniKorisnik.obavezanId());
	}

	@DeleteMapping("/{postId}")
	public PorukaDto obrisiObjavu(@PathVariable Integer postId) {
		return objaveService.obrisiObjavu(postId, trenutniKorisnik.obavezanId());
	}
}
