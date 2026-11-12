package org.etf.pumpd.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import org.etf.pumpd.entity.*;

public interface RadnikRepository extends JpaRepository<Radnik,Integer> {

	/** 12.5: svi radnici sa nalogom (bez administratora), sortirani po prezimenu pa imenu. */
	@Query("""
			SELECT r FROM Radnik r JOIN FETCH r.zaposleni z JOIN FETCH z.nalog n
			WHERE n.aktivan = true
			ORDER BY n.prezime, n.ime
			""")
	List<Radnik> sviSaNalogom();

	/** Radnik sa aktivnim nalogom; obrisan (neaktivan) radnik se tretira kao da ne postoji. */
	@Query("SELECT r FROM Radnik r JOIN FETCH r.zaposleni z JOIN FETCH z.nalog n WHERE r.id = :id AND n.aktivan = true")
	Optional<Radnik> pronadjiAktivnog(@Param("id") Integer id);
}
