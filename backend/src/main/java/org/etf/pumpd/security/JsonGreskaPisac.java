package org.etf.pumpd.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.etf.pumpd.dto.GreskaOdgovor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

/**
 * Upisuje grešku u formatu iz ugovora (ErrorResponse: status, error, message)
 * direktno u HTTP odgovor. Koristi se iz security sloja, gdje
 * GlobalExceptionHandler (@RestControllerAdvice) nije dostupan.
 */
@Component
public class JsonGreskaPisac {

	private final ObjectMapper objectMapper;

	public JsonGreskaPisac(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public void upisi(HttpServletResponse odgovor, HttpStatus status, String poruka) throws IOException {
		odgovor.setStatus(status.value());
		odgovor.setContentType(MediaType.APPLICATION_JSON_VALUE);
		odgovor.setCharacterEncoding(StandardCharsets.UTF_8.name());
		objectMapper.writeValue(odgovor.getOutputStream(),
				new GreskaOdgovor(status.value(), status.getReasonPhrase(), poruka));
	}
}
