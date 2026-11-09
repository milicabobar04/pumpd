package org.etf.pumpd.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.etf.pumpd.enums.Uloga;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

	public static final String CLAIM_ULOGA = "uloga";

	private final SecretKey kljuc;
	private final Duration trajanje;
	private final Clock clock;
	private final JwtParser parser;

	public JwtService(
			@Value("${jwt.secret}") String tajna,
			@Value("${jwt.trajanje:PT8H}") Duration trajanje,
			Clock clock) {
		this.kljuc = Keys.hmacShaKeyFor(Decoders.BASE64.decode(tajna));
		this.trajanje = trajanje;
		this.clock = clock;
		this.parser = Jwts.parser()
				.verifyWith(kljuc)
				.clock(() -> Date.from(clock.instant()))
				.build();
	}

	public String generisiToken(Integer idNalog, Uloga uloga) {
		Instant sada = clock.instant();
		return Jwts.builder()
				.id(UUID.randomUUID().toString())
				.subject(String.valueOf(idNalog))
				.claim(CLAIM_ULOGA, uloga.name())
				.issuedAt(Date.from(sada))
				.expiration(Date.from(sada.plus(trajanje)))
				.signWith(kljuc)
				.compact();
	}

	/**
	 * Provjerava potpis i rok važenja tokena i vraća idNalog (subject), jti i istek.
	 * Prazan Optional ako je token neispravan, istekao, bez jti/exp ili subject nije broj.
	 */
	public Optional<JwtPodaci> procitaj(String token) {
		try {
			Claims claims = parser.parseSignedClaims(token).getPayload();
			if (claims.getId() == null || claims.getExpiration() == null) {
				return Optional.empty();
			}
			return Optional.of(new JwtPodaci(
					Integer.valueOf(claims.getSubject()),
					claims.getId(),
					claims.getExpiration().toInstant()));
		} catch (JwtException | IllegalArgumentException e) {
			return Optional.empty();
		}
	}
}
