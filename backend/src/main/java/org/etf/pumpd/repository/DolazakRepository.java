package org.etf.pumpd.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import org.etf.pumpd.entity.Dolazak;

public interface DolazakRepository extends JpaRepository<Dolazak, Integer> {

	/**
	 * Otvoreni boravci člana (bez izlaska) koji su počeli od datog trenutka,
	 * najnoviji prvi. Servis traži one od početka današnjeg dana.
	 */
	@Query("""
			SELECT d FROM Dolazak d
			WHERE d.clan.id = :idClana
			  AND d.vrijemeIzlaska IS NULL
			  AND d.vrijemeUlaska >= :od
			ORDER BY d.vrijemeUlaska DESC
			""")
	List<Dolazak> otvoreniOd(@Param("idClana") Integer idClana, @Param("od") LocalDateTime od);

	/** 5.3: broj članova u teretani = otvoreni boravci koji su počeli od datog trenutka. */
	@Query("""
			SELECT COUNT(d) FROM Dolazak d
			WHERE d.vrijemeIzlaska IS NULL
			  AND d.vrijemeUlaska >= :od
			""")
	long brojPrisutnihOd(@Param("od") LocalDateTime od);

	/**
	 * 2.4: završeni boravci člana - sa evidentiranim izlaskom, ili započeti prije današnjeg dana
	 * (izlazak nije evidentiran). Današnji otvoreni boravak (član je još u teretani) se ne vraća.
	 */
	@Query("""
			SELECT d FROM Dolazak d
			WHERE d.clan.id = :idClana
			  AND (d.vrijemeIzlaska IS NOT NULL OR d.vrijemeUlaska < :pocetakDana)
			""")
	List<Dolazak> zavrseniBoravci(@Param("idClana") Integer idClana, @Param("pocetakDana") LocalDateTime pocetakDana);
}
