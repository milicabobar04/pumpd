package org.etf.pumpd.config;

import org.etf.pumpd.security.JsonAccessDeniedHandler;
import org.etf.pumpd.security.JsonAuthenticationEntryPoint;
import org.etf.pumpd.security.JwtAuthFilter;
import org.etf.pumpd.security.JwtService;
import org.etf.pumpd.service.KorisnikService;
import org.etf.pumpd.service.NevazeciTokenService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http,
			JwtService jwtService,
			KorisnikService korisnikService,
			NevazeciTokenService nevazeciTokenService,
			JsonAuthenticationEntryPoint entryPoint,
			JsonAccessDeniedHandler accessDeniedHandler) throws Exception {
		http
			.addFilterBefore(new JwtAuthFilter(jwtService, korisnikService, nevazeciTokenService),
				UsernamePasswordAuthenticationFilter.class)
			.csrf(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable)
			.logout(AbstractHttpConfigurer::disable)
			.sessionManagement(sesija -> sesija.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.exceptionHandling(greska -> greska
				.authenticationEntryPoint(entryPoint)
				.accessDeniedHandler(accessDeniedHandler))
			.authorizeHttpRequests(zahtjevi -> zahtjevi
				.requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
				.requestMatchers(HttpMethod.GET, "/api/gym/info").permitAll()
				.requestMatchers(HttpMethod.POST, "/api/issues").permitAll()
				.requestMatchers("/error").permitAll()
				.requestMatchers("/api/worker/attendance/**").hasRole("WORKER")          
				.requestMatchers("/api/worker/**").hasAnyRole("WORKER", "ADMIN")         
				.requestMatchers("/api/admin/**").hasRole("ADMIN")
				.requestMatchers(HttpMethod.PUT, "/api/gym/info").hasRole("ADMIN")
				.requestMatchers("/api/trainers/**").hasRole("MEMBER")
				.requestMatchers("/api/trainer/**").hasRole("TRAINER")
				.requestMatchers("/api/membership/**").hasRole("MEMBER")
				.requestMatchers("/api/training-plans/**").hasAnyRole("MEMBER", "TRAINER")
				// objave (8.x): uloge su pomiješane, pa pravila idu po metodi i tačnoj putanji;
				// pravilo za /save mora biti iznad pravila za brisanje objave
				.requestMatchers(HttpMethod.POST, "/api/posts/images").hasRole("TRAINER")
				.requestMatchers(HttpMethod.GET, "/api/images/*").permitAll()
				.requestMatchers(HttpMethod.GET, "/api/posts/feed", "/api/posts/saved").hasRole("MEMBER")
				.requestMatchers("/api/posts/*/save").hasRole("MEMBER")
				.requestMatchers(HttpMethod.GET, "/api/posts/my-posts").hasRole("TRAINER")
				.requestMatchers(HttpMethod.POST, "/api/posts").hasRole("TRAINER")
				.requestMatchers(HttpMethod.DELETE, "/api/posts/*").hasRole("TRAINER")
				// grupni treninzi (9.x): pregled termina i član i radnik, prijava i odjava samo član;
				// obrada zahtjeva (10.12, 10.13) je pod /api/trainer/** (TRAINER)
				.requestMatchers(HttpMethod.GET, "/api/group-trainings").hasAnyRole("MEMBER", "WORKER")
				.requestMatchers("/api/group-trainings/*/enrollment").hasRole("MEMBER")
				// 2.4: ostale /api/users/... rute (profil, lozinka) važe za sve uloge
				.requestMatchers(HttpMethod.GET, "/api/users/training-history").hasRole("MEMBER")
				.anyRequest().authenticated());

		return http.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
