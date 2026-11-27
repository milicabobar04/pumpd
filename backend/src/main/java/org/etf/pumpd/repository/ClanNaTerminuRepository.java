package org.etf.pumpd.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

import org.etf.pumpd.entity.*;
import org.etf.pumpd.enums.StatusPrijave;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface ClanNaTerminuRepository extends JpaRepository<ClanNaTerminu,ClanNaTerminuId>{

	Optional<ClanNaTerminu> findByIdZahtjeva(Integer idZahtjeva);
	
	@Query("""
			SELECT p FROM ClanNaTerminu p
			WHERE p.termin.id = :idTermina AND p.status IN :statusi
			""")
	List<ClanNaTerminu> prijaveNaTermin(@Param("idTermina")Integer idTermina,@Param("statusi") Collection<StatusPrijave> statusi);
	
	@Modifying
	@Query("DELETE FROM ClanNaTerminu p WHERE p.termin.id = :idTermina")
	int obrisiSveZaTermin(@Param("idTermina") Integer idTermina);

	/** Id-jevi članova čija prijava na termin ima jedan od datih statusa, bez učitavanja prijava (10.7). */
	@Query("SELECT p.id.clanId FROM ClanNaTerminu p WHERE p.termin.id = :idTermina AND p.status IN :statusi")
	List<Integer> idClanova(@Param("idTermina") Integer idTermina, @Param("statusi") Collection<StatusPrijave> statusi);

	/** 9.1: prijave jednog člana na date termine (jednim upitom). Termin bez prijave nema red. */
	@Query("SELECT p FROM ClanNaTerminu p WHERE p.id.clanId = :idClana AND p.id.terminId IN :idTermina")
	List<ClanNaTerminu> prijaveClana(@Param("idClana") Integer idClana,
			@Param("idTermina") Collection<Integer> idTermina);

	/**
	 * 10.12: zahtjevi na čekanju za termine trenera koji još nisu počeli, bez obrisanih članova,
	 * opciono samo za jedan termin. Termin, grupa, član i nalog se učitavaju istim upitom.
	 */
	@Query("""
			SELECT p FROM ClanNaTerminu p
			JOIN FETCH p.termin t JOIN FETCH t.grupa JOIN FETCH p.clan c JOIN FETCH c.nalog n
			WHERE t.trener.id = :idTrenera AND p.status = :status AND t.vrijeme > :sada AND n.aktivan = true
			  AND (:idTermina IS NULL OR t.id = :idTermina)
			ORDER BY t.vrijeme, t.id, p.datumZahtjeva, p.idZahtjeva
			""")
	List<ClanNaTerminu> zahtjeviZaTrenera(@Param("idTrenera") Integer idTrenera, @Param("status") StatusPrijave status,
			@Param("sada") LocalDateTime sada, @Param("idTermina") Integer idTermina);

	/** 10.13: trenutni status zahtjeva iz baze (ne iz keša sesije), nakon zaključavanja grupe. */
	@Query("SELECT p.status FROM ClanNaTerminu p WHERE p.idZahtjeva = :idZahtjeva")
	Optional<StatusPrijave> statusZahtjeva(@Param("idZahtjeva") Integer idZahtjeva);

	/**
	 * 2.4: prijave člana sa datim statusima na termine koji su počeli prije "sada" (servis zadržava
	 * samo završene). Termini obrisanih trenera ostaju, jer je to istorija člana.
	 */
	@Query("""
			SELECT p FROM ClanNaTerminu p JOIN FETCH p.termin t JOIN FETCH t.grupa
			WHERE p.id.clanId = :idClana AND p.status IN :statusi AND t.vrijeme < :sada
			""")
	List<ClanNaTerminu> zapocetiTerminiClana(@Param("idClana") Integer idClana,
			@Param("statusi") Collection<StatusPrijave> statusi, @Param("sada") LocalDateTime sada);
}
