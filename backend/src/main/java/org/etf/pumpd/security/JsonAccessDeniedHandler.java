package org.etf.pumpd.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** 403 kada je korisnik prijavljen, ali njegova uloga nema pravo na rutu. */
@Component
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

	private final JsonGreskaPisac pisac;

	public JsonAccessDeniedHandler(JsonGreskaPisac pisac) {
		this.pisac = pisac;
	}

	@Override
	public void handle(HttpServletRequest zahtjev, HttpServletResponse odgovor,
			AccessDeniedException izuzetak) throws IOException {
		pisac.upisi(odgovor, HttpStatus.FORBIDDEN, "Nemate dozvolu za pristup resursu.");
	}
}
