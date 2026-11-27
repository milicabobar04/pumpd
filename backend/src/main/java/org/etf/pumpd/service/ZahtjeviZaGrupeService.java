package org.etf.pumpd.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.etf.pumpd.dto.PorukaDto;
import org.etf.pumpd.dto.ZahtjevZaGrupniTreningDto;
import org.etf.pumpd.entity.ClanNaTerminu;
import org.etf.pumpd.entity.ClanUGrupi;
import org.etf.pumpd.entity.Grupa;
import org.etf.pumpd.entity.Nalog;
import org.etf.pumpd.entity.Termin;
import org.etf.pumpd.enums.OdlukaOZahtjevu;
import org.etf.pumpd.enums.StatusPrijave;
import org.etf.pumpd.exception.KonfliktException;
import org.etf.pumpd.exception.ResursNijePronadjenException;
import org.etf.pumpd.repository.ClanNaTerminuRepository;
import org.etf.pumpd.repository.ClanUGrupiRepository;
import org.etf.pumpd.repository.GrupaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/** Trenerova obrada zahtjeva za grupne treninge (10.12, 10.13). */
@Service
public class ZahtjeviZaGrupeService {

	static final String ZAHTJEV_NE_POSTOJI = "Zahtjev ne postoji.";
	static final String VEC_OBRADJEN = "Zahtjev je vec obradjen.";
	static final String GRUPA_POPUNJENA = "Grupa je popunjena.";
	/** Isti oblik vremena kao u obavještenjima B1 (10.7, 10.10). */
	private static final DateTimeFormatter FORMAT_VREMENA = DateTimeFormatter.ofPattern("dd.MM.yyyy. 'u' HH:mm");

	private final ClanNaTerminuRepository prijavaRepository;
	private final ClanUGrupiRepository clanUGrupiRepository;
	private final GrupaRepository grupaRepository;
	private final ObavjestenjeService obavjestenjeService;
	private final Clock clock;

	public ZahtjeviZaGrupeService(ClanNaTerminuRepository prijavaRepository, ClanUGrupiRepository clanUGrupiRepository,
			GrupaRepository grupaRepository, ObavjestenjeService obavjestenjeService, Clock clock) {
		this.prijavaRepository = prijavaRepository;
		this.clanUGrupiRepository = clanUGrupiRepository;
		this.grupaRepository = grupaRepository;
		this.obavjestenjeService = obavjestenjeService;
		this.clock = clock;
	}

	/** 10.12: samo zahtjevi na čekanju za termine koji još nisu počeli; tuđi sessionId daje praznu listu. */
	@Transactional(readOnly = true)
	public List<ZahtjevZaGrupniTreningDto> zahtjevi(Integer idTrenera, Integer idTermina) {
		List<ClanNaTerminu> zahtjevi = prijavaRepository.zahtjeviZaTrenera(idTrenera, StatusPrijave.NA_CEKANJU,
				LocalDateTime.now(clock), idTermina);
		if (zahtjevi.isEmpty()) {
			return List.of();
		}
		Map<Integer, Long> brojClanova = clanUGrupiRepository
				.brojClanovaPoGrupama(zahtjevi.stream().map(z -> z.getTermin().getGrupa().getId()).distinct().toList())
				.stream()
				.collect(Collectors.toMap(red -> (Integer) red[0], red -> (Long) red[1]));

		return zahtjevi.stream().map(z -> {
			Termin termin = z.getTermin();
			Grupa grupa = termin.getGrupa();
			Nalog clan = z.getClan().getNalog();
			return new ZahtjevZaGrupniTreningDto(z.getIdZahtjeva(), z.getId().getClanId(),
					clan.getIme() + " " + clan.getPrezime(), termin.getId(), grupa.getNaziv(), termin.getVrijeme(),
					brojClanova.getOrDefault(grupa.getId(), 0L), grupa.getMaxClanova(), z.getDatumZahtjeva());
		}).toList();
	}

	/**
	 * 10.13: ACCEPTED upisuje člana u grupu termina (status prijavljen), REJECTED postavlja status
	 * odbijen; član u oba slučaja dobija obavještenje. Grupa se zaključava kao u 10.10 (B1), pa
	 * istovremena prihvatanja i dodavanja ne mogu zauzeti isto posljednje mjesto; READ_COMMITTED
	 * da broj članova i status zahtjeva poslije zaključavanja budu svježi.
	 */
	@Transactional(isolation = Isolation.READ_COMMITTED)
	public PorukaDto obradi(Integer idTrenera, Integer idZahtjeva, OdlukaOZahtjevu odluka) {
		ClanNaTerminu zahtjev = prijavaRepository.findByIdZahtjeva(idZahtjeva)
				.filter(z -> z.getTermin().getTrener().getId().equals(idTrenera))
				.filter(z -> z.getClan().getNalog().isAktivan())
				.orElseThrow(() -> new ResursNijePronadjenException(ZAHTJEV_NE_POSTOJI));
		Termin termin = zahtjev.getTermin();
		Grupa grupa = grupaRepository.zakljucaj(termin.getGrupa().getId()).orElseThrow();

		// status se čita ponovo poslije zaključavanja: druga obrada ili povlačenje (9.3) je mogla završiti u međuvremenu
		StatusPrijave status = prijavaRepository.statusZahtjeva(idZahtjeva)
				.orElseThrow(() -> new ResursNijePronadjenException(ZAHTJEV_NE_POSTOJI));
		if (status != StatusPrijave.NA_CEKANJU) {
			throw new KonfliktException(VEC_OBRADJEN);
		}
		if (termin.jePoceo(LocalDateTime.now(clock))) {
			throw new KonfliktException("Termin je vec poceo.");
		}

		Integer idClana = zahtjev.getId().getClanId();
		if (odluka == OdlukaOZahtjevu.ACCEPTED) {
			if (clanUGrupiRepository.brojClanova(grupa.getId()) >= grupa.getMaxClanova()) {
				throw new KonfliktException(GRUPA_POPUNJENA);
			}
			try {
				// trigger "Grupa je puna" u bazi je zadnja zaštita kapaciteta; dupli upis istog člana nije
				// moguć jer je grupa zaključana i status provjeren, a poslije greške sesija ne smije dalje u bazu
				clanUGrupiRepository.saveAndFlush(new ClanUGrupi(zahtjev.getClan(), grupa, LocalDate.now(clock)));
			} catch (DataIntegrityViolationException e) {
				throw new KonfliktException(GRUPA_POPUNJENA);
			}
			zahtjev.postaviStatus(StatusPrijave.PRIJAVLJEN);
		} else {
			zahtjev.postaviStatus(StatusPrijave.ODBIJEN);
		}

		String odlucio = odluka == OdlukaOZahtjevu.ACCEPTED ? "prihvatio" : "odbio";
		obavjestenjeService.posalji(idClana, idTrenera, "Trener je " + odlucio + " vas zahtjev za prijavu na termin \""
				+ grupa.getNaziv() + "\" (" + FORMAT_VREMENA.format(termin.getVrijeme()) + ").");
		return new PorukaDto(odluka == OdlukaOZahtjevu.ACCEPTED ? "Zahtjev je prihvacen." : "Zahtjev je odbijen.");
	}
}
