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

}
