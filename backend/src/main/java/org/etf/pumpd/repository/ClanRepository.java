package org.etf.pumpd.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import org.etf.pumpd.entity.*;

public interface ClanRepository extends JpaRepository<Clan,Integer> {

	/**
	 * 11.1: članovi sa nalogom, opciono filtrirani po imenu, prezimenu, emailu
	 * ili punom imenu ("ime prezime"), bez obzira na velika i mala slova.
	 * search = null vraća sve članove.
	 */
	@Query("""
			SELECT c FROM Clan c JOIN FETCH c.nalog n
			WHERE n.aktivan = true
			  AND (:search IS NULL
			   OR LOWER(n.ime) LIKE LOWER(CONCAT('%', :search, '%'))
			   OR LOWER(n.prezime) LIKE LOWER(CONCAT('%', :search, '%'))
			   OR LOWER(n.email) LIKE LOWER(CONCAT('%', :search, '%'))
			   OR LOWER(CONCAT(n.ime, ' ', n.prezime)) LIKE LOWER(CONCAT('%', :search, '%')))
			ORDER BY n.prezime, n.ime
			""")
	List<Clan> pretrazi(@Param("search") String search);

	/** Član sa aktivnim nalogom; obrisan (neaktivan) član se tretira kao da ne postoji. */
	@Query("SELECT c FROM Clan c JOIN FETCH c.nalog n WHERE c.id = :id AND n.aktivan = true")
	Optional<Clan> pronadjiAktivnog(@Param("id") Integer id);
}
