package org.etf.pumpd.security;

import java.time.Instant;

/**
 * Podaci iz provjerenog JWT tokena. Filter ih postavlja kao "details"
 * autentifikacije, pa ih odjava može pročitati iz SecurityContext-a.
 */
public record JwtPodaci(Integer idNalog, String jti, Instant istice) {

}
