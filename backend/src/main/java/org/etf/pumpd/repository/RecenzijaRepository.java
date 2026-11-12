package org.etf.pumpd.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import org.etf.pumpd.entity.*;

public interface RecenzijaRepository extends JpaRepository<Recenzija,Integer> {

	/**
	 * Prosjek ocjena trenera (12.2, 7.2, 7.5, 7.6); null ako nema ocijenjenih recenzija.
	 * Recenzije obrisanih članova ostaju u prosjeku (odluka o mekom brisanju).
	 */
	@Query("SELECT AVG(r.ocjena) FROM Recenzija r WHERE r.trener.id = :idTrenera")
	Double prosjecnaOcjena(@Param("idTrenera") Integer idTrenera);

	/**
	 * 7.1: prosjeci za više trenera jednim upitom. Trener bez ocijenjenih recenzija
	 * nije u rezultatu (prosjek mu je null).
	 */
	@Query("""
			SELECT new org.etf.pumpd.repository.ProsjekOcjene(r.trener.id, AVG(r.ocjena))
			FROM Recenzija r
			WHERE r.trener.id IN :idTrenera AND r.ocjena IS NOT NULL
			GROUP BY r.trener.id
			""")
	List<ProsjekOcjene> prosjeciOcjena(@Param("idTrenera") Collection<Integer> idTrenera);

	/**
	 * 7.2, 7.6: ocijenjene recenzije trenera sa članom i nalogom (za memberName), najnovije prve.
	 * Uključene su i recenzije obrisanih članova.
	 */
	@Query("""
			SELECT r FROM Recenzija r JOIN FETCH r.clan c JOIN FETCH c.nalog
			WHERE r.trener.id = :idTrenera AND r.ocjena IS NOT NULL
			ORDER BY r.datum DESC, r.id DESC
			""")
	List<Recenzija> recenzijeTrenera(@Param("idTrenera") Integer idTrenera);

	/** 7.5: postojeća recenzija člana za trenera (najviše jedna, V7). */
	Optional<Recenzija> findByTrenerIdAndClanId(Integer idTrenera, Integer idClana);
}
