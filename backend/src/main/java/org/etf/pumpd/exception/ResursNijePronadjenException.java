package org.etf.pumpd.exception;

/**
 * Izuzetak koji se izbacuje kada traženi resurs (npr. korisnik, plan, objava)
 * ne postoji u bazi podataka.
 * <p>
 * Presreće ga {@link GlobalExceptionHandler} i mapira na HTTP status 404 Not Found.
 * </p>
 */
public class ResursNijePronadjenException extends RuntimeException {

    public ResursNijePronadjenException(String poruka) {
        super(poruka);
    }
}