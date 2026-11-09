package org.etf.pumpd.security;

import java.io.IOException;
import java.util.List;

import org.etf.pumpd.service.KorisnikService;
import org.etf.pumpd.service.NevazeciTokenService;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Čita "Authorization: Bearer <token>", provjerava token i u SecurityContext
 * postavlja autentifikaciju sa principal = idNalog i authority ROLE_<ULOGA>.
 * Odbija tokene koji su poništeni odjavom (lista nevažećih tokena).
 * Uloga se uzima iz baze (KorisnikService.odrediUlogu), ne iz tokena, pa
 * promjena uloge ili brisanje naloga važi odmah.
 * Ako token nije ispravan, zahtjev ide dalje neautentifikovan i zaštićene
 * rute vraćaju 401 preko JsonAuthenticationEntryPoint.
 *
 * Namjerno nije @Component, da je Spring Boot ne bi registrovao i kao
 * servlet filter van security lanca; instancira se u SecurityConfig.
 */
public class JwtAuthFilter extends OncePerRequestFilter {

	private static final String BEARER = "Bearer ";

	private final JwtService jwtService;
	private final KorisnikService korisnikService;
	private final NevazeciTokenService nevazeciTokenService;

	public JwtAuthFilter(JwtService jwtService, KorisnikService korisnikService,
			NevazeciTokenService nevazeciTokenService) {
		this.jwtService = jwtService;
		this.korisnikService = korisnikService;
		this.nevazeciTokenService = nevazeciTokenService;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest zahtjev, HttpServletResponse odgovor, FilterChain lanac)
			throws ServletException, IOException {

		String zaglavlje = zahtjev.getHeader(HttpHeaders.AUTHORIZATION);

		if (zaglavlje != null && zaglavlje.startsWith(BEARER)
				&& SecurityContextHolder.getContext().getAuthentication() == null) {

			String token = zaglavlje.substring(BEARER.length()).trim();

			jwtService.procitaj(token)
				.filter(podaci -> !nevazeciTokenService.jePonisten(podaci.jti()))
				.ifPresent(podaci ->
					korisnikService.odrediUlogu(podaci.idNalog()).ifPresent(uloga -> {
						var autentifikacija = new UsernamePasswordAuthenticationToken(
								String.valueOf(podaci.idNalog()),
								null,
								List.of(new SimpleGrantedAuthority("ROLE_" + uloga.name())));
						autentifikacija.setDetails(podaci);
						SecurityContextHolder.getContext().setAuthentication(autentifikacija);
					}));
		}

		lanac.doFilter(zahtjev, odgovor);
	}
}
