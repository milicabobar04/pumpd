package org.etf.pumpd.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.etf.pumpd.dto.*;
import org.etf.pumpd.service.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/gym")
public class TeretanaController {

    private final TeretanaService teretanaService;

    public TeretanaController(TeretanaService teretanaService) {
        this.teretanaService = teretanaService;
    }

    @GetMapping("/info")
    public InfoTeretanaDto informacije() {
        return teretanaService.informacije();
    }

    /** 5.2: samo ADMIN (SecurityConfig), GET ostaje javan. */
    @PutMapping("/info")
    public PorukaDto izmijeni(@Valid @RequestBody IzmjenaTeretaneZahtjev zahtjev) {
        return teretanaService.izmijeni(zahtjev);
    }
}
