package org.etf.pumpd.dto;

import org.etf.pumpd.enums.Uloga;

/** 1.1 POST /api/auth/login - odgovor 200. */
public record PrijavaOdgovor(
		Integer id,
		String name,
		String surname,
		String email,
		Uloga role,
		String token) {

}
