package org.etf.pumpd.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import org.etf.pumpd.entity.*;

public interface TrenerRepository extends JpaRepository<Trener,Integer> {

	/** svi treneri sa nalogom, sortirani po prezimenu pa imenu. */
	@Query("SELECT t FROM Trener t JOIN FETCH t.nalog n WHERE n.aktivan = true ORDER BY n.prezime, n.ime")
	List<Trener> sviSaNalogom();

	/** Trener sa aktivnim nalogom; obrisan (neaktivan) trener se tretira kao da ne postoji. */
	@Query("SELECT t FROM Trener t JOIN FETCH t.nalog n WHERE t.id = :id AND n.aktivan = true")
	Optional<Trener> pronadjiAktivnog(@Param("id") Integer id);

	/**
	 * Prosjek ocjena iz recenzija trenera; null ako nema ocijenjenih recenzija.
	 * Native upit jer entitet Recenzija još ne postoji (tok B, 7.5).
	 */
	@Query(value = "SELECT AVG(ocjena) FROM recenzija WHERE trener_nalog_idnalog = :id", nativeQuery = true)
	Double prosjecnaOcjena(@Param("id") Integer idTrenera);

	/**
	 * Broj prihvaćenih pratilaca (status 'aktivan' = ACCEPTED, vidi V2 migraciju)
	 * koji nisu obrisani (nalog.aktivan = 1, V6).
	 * Native upit jer entitet Pracenje još ne postoji (tok B, 7.3).
	 */
	@Query(value = """
			SELECT COUNT(*) FROM pracenje p
			JOIN nalog n ON n.idnalog = p.clan_nalog_idnalog
			WHERE p.trener_nalog_idnalog = :id AND p.status = 'aktivan' AND n.aktivan = 1
			""", nativeQuery = true)
	long brojPratilaca(@Param("id") Integer idTrenera);
}
