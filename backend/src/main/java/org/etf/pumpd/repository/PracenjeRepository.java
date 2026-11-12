package org.etf.pumpd.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import org.etf.pumpd.entity.*;
import org.etf.pumpd.enums.StatusPracenja;

public interface PracenjeRepository extends JpaRepository<Pracenje,Integer> {

	/** Broj redova trenera sa zadatim statusom, bez obrisanih (neaktivnih) članova. 12.2 ga zove sa ACCEPTED. */
	@Query("""
			SELECT COUNT(p) FROM Pracenje p
			WHERE p.trener.id = :idTrenera AND p.status = :status AND p.clan.nalog.aktivan = true
			""")
	long brojPratilaca(@Param("idTrenera") Integer idTrenera, @Param("status") StatusPracenja status);

	/** Red praćenja (bilo kog statusa) za par trener–član; najviše jedan, jer je par primarni ključ. */
	Optional<Pracenje> findByTrenerIdAndClanId(Integer idTrenera, Integer idClana);

	/** 10.1: zahtjevi trenera sa zadatim statusom (PENDING), bez obrisanih članova, najstariji prvi. */
	@Query("""
			SELECT p FROM Pracenje p JOIN FETCH p.clan c JOIN FETCH c.nalog n
			WHERE p.trener.id = :idTrenera AND p.status = :status AND n.aktivan = true
			ORDER BY p.datum ASC, p.id ASC
			""")
	List<Pracenje> zahtjeviTrenera(@Param("idTrenera") Integer idTrenera, @Param("status") StatusPracenja status);

	/** 10.3: članovi sa zadatim statusom (ACCEPTED) kod trenera, bez obrisanih, po prezimenu pa imenu. */
	@Query("""
			SELECT p FROM Pracenje p JOIN FETCH p.clan c JOIN FETCH c.nalog n
			WHERE p.trener.id = :idTrenera AND p.status = :status AND n.aktivan = true
			ORDER BY n.prezime, n.ime
			""")
	List<Pracenje> pratiociTrenera(@Param("idTrenera") Integer idTrenera, @Param("status") StatusPracenja status);
}
