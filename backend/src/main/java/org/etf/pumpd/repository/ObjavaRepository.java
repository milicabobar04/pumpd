package org.etf.pumpd.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.etf.pumpd.entity.Objava;
import org.etf.pumpd.enums.StatusPracenja;

public interface ObjavaRepository extends JpaRepository<Objava, Integer> {

	/** 8.5: objave trenera, najnovije prve. */
	List<Objava> findByTrenerIdOrderByVrijemeDescIdDesc(Integer idTrenera);

	/**
	 * 8.1: objave aktivnih trenera koje član prati (red praćenja sa zadatim statusom, ACCEPTED),
	 * najnovije prve. Trener i nalog se učitavaju odmah (trainerName), u istom upitu.
	 */
	@Query("""
			SELECT o FROM Objava o JOIN FETCH o.trener t JOIN FETCH t.nalog n
			WHERE n.aktivan = true
			  AND EXISTS (SELECT 1 FROM Pracenje p
			              WHERE p.trener = t AND p.clan.id = :idClana AND p.status = :status)
			ORDER BY o.vrijeme DESC, o.id DESC
			""")
	List<Objava> feed(@Param("idClana") Integer idClana, @Param("status") StatusPracenja status);
}
