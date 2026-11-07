package org.etf.pumpd.exception;

import org.etf.pumpd.dto.GreskaOdgovor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
* Globalni presretač izuzetaka u REST kontrolerima.
* <p>
* Centralizovano obrađuje sve neuhvaćene izuzetke u aplikaciji i transformiše
* ih u JSON format
* </p>
*/

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. Neočekivane greške na serveru (500)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<GreskaOdgovor> obradiOpstuGresku(Exception ex) {
        GreskaOdgovor greska = new GreskaOdgovor(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                ex.getMessage() != null ? ex.getMessage() : "Došlo je do neočekivane greške na serveru."
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
}