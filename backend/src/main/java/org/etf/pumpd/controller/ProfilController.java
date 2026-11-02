package org.etf.pumpd.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.etf.pumpd.dto.*;
import org.etf.pumpd.security.*;
import org.etf.pumpd.service.*;

@RestController
@RequestMapping("/api/users/my-profile")
public class ProfilController {

    private final ProfilService profilService;
    private final TrenutniKorisnik trenutniKorisnik;

    public ProfilController(ProfilService profilService, TrenutniKorisnik trenutniKorisnik) {
        this.profilService = profilService;
        this.trenutniKorisnik = trenutniKorisnik;
    }

    @GetMapping
    public ProfilDto mojProfil() {
        return profilService.mojProfil(trenutniKorisnik.obavezanId());
    }

    @PutMapping
    public PorukaDto izmijeniProfil(@RequestBody ProfilIzmjenaZahtjev zahtjev) {
        profilService.izmijeniProfil(trenutniKorisnik.obavezanId(), zahtjev);
        return new PorukaDto("Profil je uspješno ažuriran.");
    }
}