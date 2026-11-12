package org.etf.pumpd.service;

import static org.etf.pumpd.service.AdminTreneriService.zaokruziOcjenu;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.etf.pumpd.dto.RecenzijaDto;
import org.etf.pumpd.dto.TrenerZaClanaDetaljiDto;
import org.etf.pumpd.dto.TrenerZaClanaDto;
import org.etf.pumpd.entity.Nalog;
import org.etf.pumpd.entity.Pracenje;
import org.etf.pumpd.entity.Recenzija;
import org.etf.pumpd.entity.Trener;
import org.etf.pumpd.enums.StatusPracenjaClana;
import org.etf.pumpd.exception.ResursNijePronadjenException;
import org.etf.pumpd.repository.PracenjeRepository;
import org.etf.pumpd.repository.ProsjekOcjene;
import org.etf.pumpd.repository.RecenzijaRepository;
import org.etf.pumpd.repository.TrenerRepository;

/** Treneri iz ugla člana: rute 7.1–7.5. Obrisani (neaktivni) treneri za člana ne postoje. */
@Service
public class TreneriService {

	private final TrenerRepository trenerRepository;
	private final RecenzijaRepository recenzijaRepository;
	private final PracenjeRepository pracenjeRepository;

	public TreneriService(TrenerRepository trenerRepository, RecenzijaRepository recenzijaRepository,
			PracenjeRepository pracenjeRepository) {
		this.trenerRepository = trenerRepository;
		this.recenzijaRepository = recenzijaRepository;
		this.pracenjeRepository = pracenjeRepository;
	}

	/** 7.1: aktivni treneri sa prosječnom ocjenom; prosjeci se čitaju jednim upitom. */
	@Transactional(readOnly = true)
	public List<TrenerZaClanaDto> listaTrenera() {
		List<Trener> treneri = trenerRepository.sviSaNalogom();
		if (treneri.isEmpty()) {
			return List.of();
		}

		List<Integer> idTrenera = treneri.stream().map(t -> t.getNalog().getId()).toList();
		Map<Integer, Double> prosjeci = recenzijaRepository.prosjeciOcjena(idTrenera).stream()
				.collect(Collectors.toMap(ProsjekOcjene::idTrenera, ProsjekOcjene::prosjek));

		return treneri.stream()
				.map(Trener::getNalog)
				.map(n -> new TrenerZaClanaDto(n.getId(), n.getIme(), n.getPrezime(),
						zaokruziOcjenu(prosjeci.get(n.getId()))))
				.toList();
	}

	/** 7.2: detalji trenera sa followStatus za prijavljenog člana i recenzijama. */
	@Transactional(readOnly = true)
	public TrenerZaClanaDetaljiDto detaljiTrenera(Integer trainerId, Integer idClana) {
		Trener trener = trenerRepository.pronadjiAktivnog(trainerId)
				.orElseThrow(() -> new ResursNijePronadjenException("Trener ne postoji."));
		Nalog nalog = trener.getNalog();

		StatusPracenjaClana followStatus = StatusPracenjaClana.iz(
				pracenjeRepository.findByTrenerIdAndClanId(trainerId, idClana)
						.map(Pracenje::getStatus)
						.orElse(null));

		List<RecenzijaDto> recenzije = recenzijaRepository.recenzijeTrenera(trainerId).stream()
				.map(TreneriService::uDto)
				.toList();

		return new TrenerZaClanaDetaljiDto(nalog.getId(), nalog.getIme(), nalog.getPrezime(), trener.getOpis(),
				zaokruziOcjenu(recenzijaRepository.prosjecnaOcjena(trainerId)), followStatus, recenzije);
	}

	/** Isti oblik recenzije koristi i 7.6. */
	static RecenzijaDto uDto(Recenzija r) {
		Nalog clan = r.getClan().getNalog();
		return new RecenzijaDto(r.getId(), clan.getIme() + " " + clan.getPrezime(), r.getOcjena(),
				r.getKomentar(), r.getDatum());
	}
}
