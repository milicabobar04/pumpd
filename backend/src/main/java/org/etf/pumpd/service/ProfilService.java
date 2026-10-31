package org.etf.pumpd.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import org.etf.pumpd.dto.*;
import org.etf.pumpd.repository.*;
import org.etf.pumpd.entity.*;
import org.etf.pumpd.enums.*;


@Service
public class ProfilService {

	

	public ProfilService(NalogRepository nalogRepository, ClanRepository clanRepository,
			KorisnikService korisnikService, Clock clock) {
		super();
		this.nalogRepository = nalogRepository;
		this.clanRepository = clanRepository;
		this.korisnikService = korisnikService;
		this.clock = clock;
	}

	static final int NAJRANIJA_GODINA_RODJENJA = 1900;
    static final int MAX_DUZINA_IMENA = 45;
    static final double MAX_TEZINA_KG = 500;
    static final double MAX_VISINA_CM = 300;

    private final NalogRepository nalogRepository;
    private final ClanRepository clanRepository;
    private final KorisnikService korisnikService;
    private final Clock clock;
	
	
    @Transactional(readOnly = true)
    public ProfilDto mojProfil(Integer idNalog) {
        Nalog nalog = ucitajNalog(idNalog);
        Uloga uloga = ucitajUlogu(idNalog);

        Integer godinaRodjenja = null;
        Double tezina = null;
        Double visina = null;
        if (uloga == Uloga.MEMBER) {
            Clan clan = ucitajClana(idNalog);
            godinaRodjenja = clan.getDatumRodjenja() == null ? null : clan.getDatumRodjenja().getYear();
            tezina = clan.getTjelesnaTezina();
            visina = clan.getVisina();
        }
        return new ProfilDto(nalog.getId(), nalog.getIme(), nalog.getPrezime(), nalog.getEmail(),
                godinaRodjenja, tezina, visina, uloga);
    }
    
    @Transactional
    public void izmijeniProfil(Integer idNalog, ProfilIzmjenaZahtjev zahtjev) {
        String ime = obaveznoIme(zahtjev.name(), "Ime");
        String prezime = obaveznoIme(zahtjev.surname(), "Prezime");

        Nalog nalog = ucitajNalog(idNalog);
        Uloga uloga = ucitajUlogu(idNalog);

        if (uloga == Uloga.MEMBER) {
            provjeriMjere(zahtjev);
            Clan clan = ucitajClana(idNalog);
            clan.setTjelesnaTezina(zahtjev.weight());
            clan.setVisina(zahtjev.height());
            clan.setDatumRodjenja(datumIzGodine(zahtjev.birthYear(), clan.getDatumRodjenja()));
            clanRepository.save(clan);
        }

        nalog.setIme(ime);
        nalog.setPrezime(prezime);
        nalogRepository.save(nalog);
    }

    private void provjeriMjere(ProfilIzmjenaZahtjev zahtjev) {
        Integer godina = zahtjev.birthYear();
        int trenutnaGodina = Year.now(clock).getValue();
        if (godina != null && (godina < NAJRANIJA_GODINA_RODJENJA || godina > trenutnaGodina)) {
            throw neispravno("Godina rodjenja mora biti izmedju " + NAJRANIJA_GODINA_RODJENJA
                    + " i " + trenutnaGodina + ".");
        }
        Double tezina = zahtjev.weight();
        if (tezina != null && (tezina <= 0 || tezina > MAX_TEZINA_KG)) {
            throw neispravno("Tezina mora biti veca od 0 i najvise " + (int) MAX_TEZINA_KG + " kg.");
        }
        Double visina = zahtjev.height();
        if (visina != null && (visina <= 0 || visina > MAX_VISINA_CM)) {
            throw neispravno("Visina mora biti veca od 0 i najvise " + (int) MAX_VISINA_CM + " cm.");
        }
    }
    
    private LocalDate datumIzGodine(Integer godina, LocalDate postojeci) {
        if (godina == null) {
            return null;
        }
        if (postojeci != null && postojeci.getYear() == godina) {
            return postojeci;
        }
        return LocalDate.of(godina, 1, 1);
    }

    
    private String obaveznoIme(String vrijednost, String naziv) {
        String ocisceno = vrijednost == null ? "" : vrijednost.trim();
        if (ocisceno.isEmpty()) {
            throw neispravno(naziv + " je obavezno.");
        }
        if (ocisceno.length() > MAX_DUZINA_IMENA) {
            throw neispravno(naziv + " moze imati najviše " + MAX_DUZINA_IMENA + " znakova.");
        }
        return ocisceno;
    }
    
    private Nalog ucitajNalog(Integer idNalog) {
        return nalogRepository.findById(idNalog).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Nalog vise ne postoji."));
    }

    private Uloga ucitajUlogu(Integer idNalog) {
        return korisnikService.odrediUlogu(idNalog).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Nalog nema dodijeljenu ulogu."));
    }

    private Clan ucitajClana(Integer idNalog) {
        return clanRepository.findById(idNalog).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Clan ne postoji."));
    }

    private ResponseStatusException neispravno(String poruka) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, poruka);
    }
    

}