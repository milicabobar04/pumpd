package org.etf.pumpd.exception;

import org.etf.pumpd.dto.GreskaOdgovor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

/**
* Globalni presretač izuzetaka u REST kontrolerima.
* <p>
* Centralizovano obrađuje sve neuhvaćene izuzetke u aplikaciji i transformiše
* ih u JSON format
* </p>
*/

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 1. Sve ostale greške.
    // Springove MVC greške (ErrorResponse: nepostojeća ruta 404, pogrešna metoda 405,
    // nepodržan Content-Type 415, ...) zadržavaju svoj status.
    // Prave neočekivane greške daju 500 sa opštom porukom; detalji idu samo u log,
    // da interni podaci (npr. SQL greške) ne bi stigli do klijenta.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<GreskaOdgovor> obradiOpstuGresku(Exception ex) {
        if (ex instanceof ErrorResponse springGreska) {
            HttpStatusCode kod = springGreska.getStatusCode();
            HttpStatus status = HttpStatus.resolve(kod.value());
            String naziv = status != null ? status.getReasonPhrase() : String.valueOf(kod.value());
            String detalj = springGreska.getBody().getDetail();
            GreskaOdgovor greska = new GreskaOdgovor(kod.value(), naziv, detalj != null ? detalj : naziv);
            return new ResponseEntity<>(greska, kod);
        }

        log.error("Neočekivana greška na serveru", ex);
        GreskaOdgovor greska = new GreskaOdgovor(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "Došlo je do neočekivane greške na serveru."
        );
        return new ResponseEntity<>(greska, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // 2. Resurs nije pronađen - 404 Not Found
    @ExceptionHandler(ResursNijePronadjenException.class)
    public ResponseEntity<GreskaOdgovor> obradiResursNijePronadjen(ResursNijePronadjenException ex) {
        GreskaOdgovor greska = new GreskaOdgovor(
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                ex.getMessage()
        );
        return new ResponseEntity<>(greska, HttpStatus.NOT_FOUND);
    }

    // 3. Nevalidni podaci u zahtjevu - 400 Bad Request
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GreskaOdgovor> obradiValidaciju(MethodArgumentNotValidException ex) {
        String prvaGreska = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .findFirst()
                .orElse("Neispravni ili nevalidni podaci u zahtjevu.");

        GreskaOdgovor greska = new GreskaOdgovor(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                prvaGreska
        );
        return new ResponseEntity<>(greska, HttpStatus.BAD_REQUEST);
    }
    
    
    // 4. Konflikt - 409 Not Conflict (dupli zahtjevi, vec obradjene akcije...)
    @ExceptionHandler(KonfliktException.class)
    public ResponseEntity<GreskaOdgovor> obradiKonflikt(KonfliktException ex) {
        GreskaOdgovor greska = new GreskaOdgovor(
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                ex.getMessage()
        );
        return new ResponseEntity<>(greska, HttpStatus.CONFLICT);
    }
    
 // 5. Pristup odbijen - 403 Forbidden (JWT autorizacija nije prosla)
    // (method security, npr. @PreAuthorize; isti format kao JsonAccessDeniedHandler)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<GreskaOdgovor> obradiAccessDenied(AccessDeniedException ex) {
        GreskaOdgovor greska = new GreskaOdgovor(
                HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                "Nemate dozvolu za pristup resursu."
        );
        return new ResponseEntity<>(greska, HttpStatus.FORBIDDEN);
    }
    
    // 6. ResponseStatusException
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<GreskaOdgovor> obradiResponseStatus(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        GreskaOdgovor greska = new GreskaOdgovor(
                status.value(),
                status.getReasonPhrase(),
                ex.getReason() != null ? ex.getReason() : status.getReasonPhrase()
        );
        return new ResponseEntity<>(greska, ex.getStatusCode());
    }
    
    // 7. Los JSON format -400 Bad Request
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<GreskaOdgovor> obradiLosFormat(Exception ex) {
        GreskaOdgovor greska = new GreskaOdgovor(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Format zahtjeva ili tip parametra u zahtjevu neispravan."
        );
        return new ResponseEntity<>(greska, HttpStatus.BAD_REQUEST);
    }

    // 8. Pogrešan email ili lozinka - 401 Unauthorized (1.1 POST /api/auth/login)
    @ExceptionHandler(NeispravnaPrijavaException.class)
    public ResponseEntity<GreskaOdgovor> obradiNeispravnuPrijavu(NeispravnaPrijavaException ex) {
        GreskaOdgovor greska = new GreskaOdgovor(
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                ex.getMessage()
        );
        return new ResponseEntity<>(greska, HttpStatus.UNAUTHORIZED);
    }
}
