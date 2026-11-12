package org.etf.pumpd.service;

import static org.etf.pumpd.service.AdminTreneriService.zaokruziOcjenu;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.etf.pumpd.dto.RecenzijaOdgovor;
import org.etf.pumpd.dto.RecenzijaZahtjev;
import org.etf.pumpd.dto.RecenzijeTreneraDto;
import org.etf.pumpd.entity.Clan;
import org.etf.pumpd.entity.Nalog;
import org.etf.pumpd.entity.Recenzija;
import org.etf.pumpd.entity.Trener;
import org.etf.pumpd.exception.KonfliktException;
import org.etf.pumpd.exception.ResursNijePronadjenException;
import org.etf.pumpd.repository.ClanRepository;
import org.etf.pumpd.repository.RecenzijaRepository;
import org.etf.pumpd.repository.TrenerRepository;

/** Recenzije trenera: ostavljanje (7.5) i trenerov pregled (7.6). */
@Service
public class RecenzijeService {

	private final RecenzijaRepository recenzijaRepository;
	private final TrenerRepository trenerRepository;
	private final ClanRepository clanRepository;
	private final ObavjestenjeService obavjestenjeService;
	private final Clock clock;

	public RecenzijeService(RecenzijaRepository recenzijaRepository, TrenerRepository trenerRepository,
			ClanRepository clanRepository, ObavjestenjeService obavjestenjeService, Clock clock) {
		this.recenzijaRepository = recenzijaRepository;
		this.trenerRepository = trenerRepository;
		this.clanRepository = clanRepository;
		this.obavjestenjeService = obavjestenjeService;
		this.clock = clock;
	}

	/**
	 * 7.5: član ocjenjuje aktivnog trenera (praćenje nije uslov). Član ima najviše jednu
	 * recenziju po treneru: nova zamjenjuje staru i zadržava isti reviewId.
	 * Odgovor sadrži novi prosjek, zaokružen na jednu decimalu.
	 * Trener dobija obavještenje sa ocjenom (bez komentara), u istoj transakciji.
	 */
	@Transactional
	public RecenzijaOdgovor ostaviRecenziju(Integer trainerId, Integer idClana, RecenzijaZahtjev zahtjev) {
		Trener trener = trenerRepository.pronadjiAktivnog(trainerId)
				.orElseThrow(() -> new ResursNijePronadjenException("Trener ne postoji."));
		Clan clan = clanRepository.pronadjiAktivnog(idClana)
				.orElseThrow(() -> new ResursNijePronadjenException("Clan ne postoji."));
		String komentar = ocistiKomentar(zahtjev.comment());
		LocalDate danas = LocalDate.now(clock);

		Optional<Recenzija> postojeca = recenzijaRepository.findByTrenerIdAndClanId(trainerId, idClana);
		Recenzija recenzija;
		String poruka;
		String radnja;
		if (postojeca.isPresent()) {
			recenzija = postojeca.get();
			recenzija.izmijeni(zahtjev.grade(), komentar, danas);
			recenzija = recenzijaRepository.saveAndFlush(recenzija);
			poruka = "Recenzija je azurirana.";
			radnja = " je izmijenio recenziju (ocjena: ";
		} else {
			try {
				// saveAndFlush odmah šalje INSERT: novi prosjek uključuje ovu ocjenu, a UNIQUE (V7)
				// kod dvostrukog klika puca ovdje
				recenzija = recenzijaRepository.saveAndFlush(
						new Recenzija(trener, clan, zahtjev.grade(), komentar, danas));
			} catch (DataIntegrityViolationException e) {
				throw new KonfliktException("Recenzija se upravo cuva, pokusajte ponovo.");
			}
			poruka = "Recenzija je sacuvana.";
			radnja = " vam je ostavio recenziju (ocjena: ";
		}

		Nalog nalogClana = clan.getNalog();
		obavjestenjeService.posalji(trainerId, idClana,
				nalogClana.getIme() + " " + nalogClana.getPrezime() + radnja + zahtjev.grade() + ").");

		return new RecenzijaOdgovor(recenzija.getId(),
				zaokruziOcjenu(recenzijaRepository.prosjecnaOcjena(trainerId)), poruka);
	}

	/** 7.6: prosjek i recenzije prijavljenog trenera (isti oblik kao u 7.2). */
	@Transactional(readOnly = true)
	public RecenzijeTreneraDto recenzijeTrenera(Integer idTrenera) {
		return new RecenzijeTreneraDto(
				zaokruziOcjenu(recenzijaRepository.prosjecnaOcjena(idTrenera)),
				recenzijaRepository.recenzijeTrenera(idTrenera).stream().map(TreneriService::uDto).toList());
	}

	/** Prazan komentar (ili samo razmaci) se čuva kao null. */
	private String ocistiKomentar(String komentar) {
		if (komentar == null) {
			return null;
		}
		String ociscen = komentar.trim();
		return ociscen.isEmpty() ? null : ociscen;
	}
}
