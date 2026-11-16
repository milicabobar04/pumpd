package org.etf.pumpd.repository;

import java.util.List;

import org.etf.pumpd.entity.PlanTreninga;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlanTreningaRepository extends JpaRepository<PlanTreninga,Integer> {

	@Query("""
			SELECT p FROM PlanTreninga p
			WHERE p.idTrenera = :idKorisnika OR p.idClana = :idKorisnika
			ORDER BY p.datumKreiranja DESC, p.id DESC
			"""
			)
	List<PlanTreninga> planoviKorisnika(@Param("idKorisnika")Integer idKorisnika);
}
