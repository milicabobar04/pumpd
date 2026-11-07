package org.etf.pumpd.dto;

/**
 * DTO klasa koja predstavlja standardizovani format odgovora u slučaju greške.
 */
public class GreskaOdgovor {

    // HTTP status kod greške 
    private int status;

    // Skraćeni tekstualni naziv HTTP greške  
    private String error;

    // Detaljna poruka o grešci 
    private String message;


    public GreskaOdgovor() {
    }

    public GreskaOdgovor(int status, String error, String message) {
        this.status = status;
        this.error = error;
        this.message = message;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}