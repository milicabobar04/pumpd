package org.etf.pumpd.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.etf.pumpd.dto.DodavanjeRadnikaOdgovor;
import org.etf.pumpd.dto.DodavanjeRadnikaZahtjev;
import org.etf.pumpd.dto.PorukaDto;
import org.etf.pumpd.dto.RadnikDto;
import org.etf.pumpd.entity.Nalog;
import org.etf.pumpd.entity.Radnik;
import org.etf.pumpd.entity.Zaposleni;
import org.etf.pumpd.exception.ResursNijePronadjenException;
import org.etf.pumpd.repository.RadnikRepository;
import org.etf.pumpd.repository.ZaposleniRepository;

/** Upravljanje radnicima za administratora, rute 12.5–12.8. */
@Service
public class AdminRadniciService {

	private final RadnikRepository radnikRepository;
	private final ZaposleniRepository zaposleniRepository;
	private final NoviNalogService noviNalogService;

	public AdminRadniciService(RadnikRepository radnikRepository, ZaposleniRepository zaposleniRepository,
			NoviNalogService noviNalogService) {
		this.radnikRepository = radnikRepository;
		this.zaposleniRepository = zaposleniRepository;
		this.noviNalogService = noviNalogService;
	}

	/** 12.5: lista svih radnika. */
	@Transactional(readOnly = true)
	public List<RadnikDto> listaRadnika() {
		return radnikRepository.sviSaNalogom().stream()
				.map(AdminRadniciService::uDto)
				.toList();
	}

	/** 12.6: detalji radnika. Id koji ne pripada radniku (npr. administratoru) daje 404. */
	@Transactional(readOnly = true)
	public RadnikDto detaljiRadnika(Integer workerId) {
		return uDto(ucitajAktivnogRadnika(workerId));
	}

	/**
	 * 12.7: novi nalog sa ulogom WORKER. Upisuje se u tri tabele (nalog, zaposleni, radnik)
	 * u istoj transakciji.
	 */
	@Transactional
	public DodavanjeRadnikaOdgovor dodajRadnika(DodavanjeRadnikaZahtjev zahtjev) {
		NoviNalogService.NoviNalog novi = noviNalogService.kreirajNalog(zahtjev.name(), zahtjev.surname(),
				zahtjev.email());
		Nalog nalog = novi.nalog();
		Zaposleni zaposleni = zaposleniRepository.save(new Zaposleni(nalog));
		radnikRepository.save(new Radnik(zaposleni));
		return new DodavanjeRadnikaOdgovor(nalog.getId(), novi.privremenaLozinka(), "Radnik je uspjesno dodan.");
	}

	/**
	 * 12.8: meko brisanje radnika (nalog.aktivan = false, email se oslobađa).
	 * Nalog je učitan u ovoj transakciji, pa se izmjena upisuje pri njenom završetku.
	 */
	@Transactional
	public PorukaDto ukloniRadnika(Integer workerId) {
		ucitajAktivnogRadnika(workerId).getZaposleni().getNalog().deaktiviraj();
		return new PorukaDto("Radnik je uspjesno uklonjen.");
	}

	/** Obrisan (neaktivan) radnik ili id koji ne pripada radniku daju 404. */
	private Radnik ucitajAktivnogRadnika(Integer workerId) {
		return radnikRepository.pronadjiAktivnog(workerId)
				.orElseThrow(() -> new ResursNijePronadjenException("Radnik ne postoji."));
	}

	private static RadnikDto uDto(Radnik radnik) {
		Nalog n = radnik.getZaposleni().getNalog();
		return new RadnikDto(n.getId(), n.getIme(), n.getPrezime(), n.getEmail());
	}
}
