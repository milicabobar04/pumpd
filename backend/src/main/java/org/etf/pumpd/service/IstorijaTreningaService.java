package org.etf.pumpd.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Stream;

import org.etf.pumpd.dto.TreningIzIstorijeDto;
import org.etf.pumpd.entity.Dolazak;
import org.etf.pumpd.entity.Termin;
import org.etf.pumpd.enums.StatusPrijave;
import org.etf.pumpd.enums.VrstaTreninga;
import org.etf.pumpd.repository.ClanNaTerminuRepository;
import org.etf.pumpd.repository.DolazakRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 2.4: istorija treninga člana iz prošlih grupnih termina i evidentiranih dolazaka. */
@Service
public class IstorijaTreningaService {

	static final String SAMOSTALNI_TRENING = "Samostalni trening";

	private final ClanNaTerminuRepository prijavaRepository;
	private final DolazakRepository dolazakRepository;
	private final Clock clock;

	public IstorijaTreningaService(ClanNaTerminuRepository prijavaRepository, DolazakRepository dolazakRepository,
			Clock clock) {
		this.prijavaRepository = prijavaRepository;
		this.dolazakRepository = dolazakRepository;
		this.clock = clock;
	}

	/** Trening sa tačnim vremenom početka, za sortiranje (u odgovor ide samo datum). */
	private record Stavka(LocalDateTime pocetak, int id, TreningIzIstorijeDto dto) {
	}

	/** Vrijeme od početka do kraja grupnog termina. */
	private record Interval(LocalDateTime od, LocalDateTime doVremena) {
	}

	/**
	 * GROUP: završeni termini na kojima je član bio prijavljen. SOLO: završeni dolasci, osim onih
	 * koji se vremenski preklapaju sa nekim od tih termina (član je tada došao na grupni trening, pa
	 * se ne broji dvaput). Jutarnji samostalni trening i večernji grupni istog dana su dva treninga.
	 * Od najnovijeg; u istom trenutku viši id prvi, da redoslijed bude stalan.
	 */
	@Transactional(readOnly = true)
	public List<TreningIzIstorijeDto> istorija(Integer idClana) {
		LocalDateTime sada = LocalDateTime.now(clock);
		List<Termin> zavrseniTermini = prijavaRepository
				.zapocetiTerminiClana(idClana, EnumSet.of(StatusPrijave.PRIJAVLJEN, StatusPrijave.PRISUTAN), sada)
				.stream()
				.map(p -> p.getTermin())
				.filter(t -> !kraj(t).isAfter(sada))
				.toList();
		List<Interval> termini = zavrseniTermini.stream().map(t -> new Interval(t.getVrijeme(), kraj(t))).toList();
		Stream<Stavka> grupni = zavrseniTermini.stream().map(IstorijaTreningaService::grupni);
		Stream<Stavka> samostalni = dolazakRepository.zavrseniBoravci(idClana, sada.toLocalDate().atStartOfDay())
				.stream()
				.filter(d -> termini.stream().noneMatch(t -> preklapaSe(d, t)))
				.map(IstorijaTreningaService::samostalni);

		return Stream.concat(grupni, samostalni)
				.sorted(Comparator.comparing(Stavka::pocetak).thenComparing(Stavka::id).reversed())
				.map(Stavka::dto)
				.toList();
	}

	/**
	 * Dolazak se preklapa sa terminom ako je član ušao prije kraja termina i izašao poslije njegovog
	 * početka. Bez evidentiranog izlaska ne zna se kad je otišao, pa se računa kraj dana ulaska:
	 * ulazak tog dana prije kraja termina znači da je došao na taj termin.
	 */
	private static boolean preklapaSe(Dolazak dolazak, Interval termin) {
		LocalDateTime ulazak = dolazak.getVrijemeUlaska();
		LocalDateTime izlazak = dolazak.getVrijemeIzlaska() != null ? dolazak.getVrijemeIzlaska()
				: ulazak.toLocalDate().plusDays(1).atStartOfDay();
		return ulazak.isBefore(termin.doVremena()) && izlazak.isAfter(termin.od());
	}

	/** Kolona Trajanje u bazi dozvoljava NULL; termin bez trajanja se računa kao završen kad počne. */
	private static LocalDateTime kraj(Termin termin) {
		return termin.getTrajanjeMinuta() == null ? termin.getVrijeme() : termin.getKraj();
	}

	private static Stavka grupni(Termin termin) {
		return new Stavka(termin.getVrijeme(), termin.getId(), new TreningIzIstorijeDto(termin.getId(),
				termin.getGrupa().getNaziv(), termin.getVrijeme().toLocalDate(), VrstaTreninga.GROUP));
	}

	private static Stavka samostalni(Dolazak dolazak) {
		return new Stavka(dolazak.getVrijemeUlaska(), dolazak.getId(), new TreningIzIstorijeDto(dolazak.getId(),
				SAMOSTALNI_TRENING, dolazak.getVrijemeUlaska().toLocalDate(), VrstaTreninga.SOLO));
	}
}
