package org.etf.pumpd.security;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.GrantedAuthority;
import org.etf.pumpd.enums.Uloga;

@Component
public class TrenutniKorisnik {

	 public Optional<Integer> id() {
	        Authentication autentifikacija = SecurityContextHolder.getContext().getAuthentication();
	        if (autentifikacija == null
	                || !autentifikacija.isAuthenticated()
	                || autentifikacija instanceof AnonymousAuthenticationToken) {
	            return Optional.empty();
	        }
	        try {
	            return Optional.of(Integer.valueOf(autentifikacija.getName()));
	        } catch (NumberFormatException e) {
	            return Optional.empty();
	        }
	    }


	    public Integer obavezanId() {
	        return id().orElseThrow(
	                () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Potrebna je prijava."));
	    }
	    
	    public Uloga obaveznaUloga() {
			Authentication autentifikacija = SecurityContextHolder.getContext().getAuthentication();
			if (autentifikacija == null || autentifikacija instanceof AnonymousAuthenticationToken) {
				throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Potrebna je prijava.");
			}
			return autentifikacija.getAuthorities().stream()
					.map(GrantedAuthority::getAuthority)
					.filter(a -> a.startsWith("ROLE_"))
					.map(a -> Uloga.valueOf(a.substring("ROLE_".length())))
					.findFirst()
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Potrebna je prijava."));
		}
}
