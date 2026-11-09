package org.etf.pumpd.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;

import org.etf.pumpd.dto.*;
import org.etf.pumpd.security.JwtPodaci;
import org.etf.pumpd.service.AuthService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	public AuthController(AuthService authService) {
		super();
		this.authService = authService;
	}

	private final AuthService authService;

	/** 1.1 Javna ruta. 400 za nevalidan zahtjev, 401 za pogrešan email ili lozinku. */
	@PostMapping("/login")
	public PrijavaOdgovor prijava(@Valid @RequestBody PrijavaZahtjev zahtjev)
	{
		return authService.prijava(zahtjev);
	}

	/** 1.2 Zahtijeva važeći token; nakon odjave taj token više ne prolazi filter. */
	@PostMapping("/logout")
	public PorukaDto odjava(Authentication autentifikacija)
	{
		if (autentifikacija == null || !(autentifikacija.getDetails() instanceof JwtPodaci podaci)) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Potrebna je prijava.");
		}
		authService.odjava(podaci);
		return new PorukaDto("Uspješna odjava.");
	}
}
