package org.etf.pumpd.dto;

import org.etf.pumpd.enums.Uloga;

/**
 * 1.1 POST /api/auth/login - odgovor 200. mustChangePassword = true znači da korisnik ima
 * privremenu lozinku i aplikacija treba otvoriti ekran za promjenu (PUT /api/users/my-password).
 */
public record PrijavaOdgovor(
		Integer id,
		String name,
		String surname,
		String email,
		Uloga role,
		String token,
		boolean mustChangePassword) {

}
