package org.etf.pumpd.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** 401 kada zaštićenoj ruti pristupa neprijavljen korisnik (nema, neispravan, istekao ili poništen token). */
@Component
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final JsonGreskaPisac pisac;

	public JsonAuthenticationEntryPoint(JsonGreskaPisac pisac) {
		this.pisac = pisac;
	}

	@Override
	public void commence(HttpServletRequest zahtjev, HttpServletResponse odgovor,
			AuthenticationException izuzetak) throws IOException {
		pisac.upisi(odgovor, HttpStatus.UNAUTHORIZED, "Potrebna je prijava.");
	}
}
