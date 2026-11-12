package org.etf.pumpd.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.etf.pumpd.dto.DodavanjeTreneraOdgovor;
import org.etf.pumpd.dto.DodavanjeTreneraZahtjev;
import org.etf.pumpd.dto.PorukaDto;
import org.etf.pumpd.dto.TrenerDetaljiDto;
import org.etf.pumpd.dto.TrenerListaDto;
import org.etf.pumpd.entity.Nalog;
import org.etf.pumpd.entity.Trener;
import org.etf.pumpd.exception.ResursNijePronadjenException;
import org.etf.pumpd.repository.TrenerRepository;

/** Upravljanje trenerima za administratora, rute 12.1–12.4. */
@Service
public class AdminTreneriService {

	private final TrenerRepository trenerRepository;
	private final NoviNalogService noviNalogService;

	public AdminTreneriService(TrenerRepository trenerRepository, NoviNalogService noviNalogService) {
		this.trenerRepository = trenerRepository;
		this.noviNalogService = noviNalogService;
	}

	/** 12.1: lista svih trenera. */
	@Transactional(readOnly = true)
	public List<TrenerListaDto> listaTrenera() {
		return trenerRepository.sviSaNalogom().stream()
				.map(Trener::getNalog)
				.map(n -> new TrenerListaDto(n.getId(), n.getIme(), n.getPrezime(), n.getEmail()))
				.toList();
	}

	/** 12.2: detalji trenera. Id koji ne pripada treneru daje 404. */
	@Transactional(readOnly = true)
	public TrenerDetaljiDto detaljiTrenera(Integer trainerId) {
		Trener trener = ucitajAktivnogTrenera(trainerId);
		Nalog nalog = trener.getNalog();

		return new TrenerDetaljiDto(
				nalog.getId(),
				nalog.getIme(),
				nalog.getPrezime(),
				nalog.getEmail(),
				trener.getOpis(),
				zaokruziOcjenu(trenerRepository.prosjecnaOcjena(trainerId)),
				Math.toIntExact(trenerRepository.brojPratilaca(trainerId)));
	}

	/** 12.3: novi nalog sa ulogom TRAINER. Nalog i trener se upisuju u istoj transakciji. */
	@Transactional
	public DodavanjeTreneraOdgovor dodajTrenera(DodavanjeTreneraZahtjev zahtjev) {
		Nalog nalog = noviNalogService.kreirajNalog(zahtjev.name(), zahtjev.surname(), zahtjev.email(),
				zahtjev.password());
		trenerRepository.save(new Trener(nalog, null, null, ocistiBiografiju(zahtjev.biography())));
		return new DodavanjeTreneraOdgovor(nalog.getId(), "Trener je uspjesno dodan.");
	}

	/**
	 * 12.4: meko brisanje trenera (nalog.aktivan = false, email se oslobađa).
	 * Objave, termini, grupe, planovi i recenzije trenera ostaju zbog istorije.
	 * Nalog je učitan u ovoj transakciji, pa se izmjena upisuje pri njenom završetku.
	 */
	@Transactional
	public PorukaDto ukloniTrenera(Integer trainerId) {
		ucitajAktivnogTrenera(trainerId).getNalog().deaktiviraj();
		return new PorukaDto("Trener je uspjesno uklonjen.");
	}

	/** Obrisan (neaktivan) trener ili id koji ne pripada treneru daju 404. */
	private Trener ucitajAktivnogTrenera(Integer trainerId) {
		return trenerRepository.pronadjiAktivnog(trainerId)
				.orElseThrow(() -> new ResursNijePronadjenException("Trener ne postoji."));
	}

	/** Prazna biografija (ili samo razmaci) se čuva kao null. */
	private String ocistiBiografiju(String biografija) {
		if (biografija == null) {
			return null;
		}
		String ociscena = biografija.trim();
		return ociscena.isEmpty() ? null : ociscena;
	}

	/**
	 * Prosječna ocjena se prikazuje na jednu decimalu (4.3333 -> 4.3).
	 */
	public static Double zaokruziOcjenu(Double prosjek) {
		if (prosjek == null) {
			return null;
		}
		return Math.round(prosjek * 10) / 10.0;
	}
}
