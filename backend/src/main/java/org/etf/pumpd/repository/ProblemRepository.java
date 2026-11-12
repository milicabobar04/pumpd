package org.etf.pumpd.repository;

import java.util.List;

import org.springframework.data.jpa.repository.*;

import org.etf.pumpd.entity.*;
import org.etf.pumpd.enums.StatusProblema;

public interface ProblemRepository extends JpaRepository<Problem,Integer> {

	/** 4.2 bez filtera: najnoviji problemi prvi. */
	List<Problem> findAllByOrderByVrijemeDescIdDesc();

	/** 4.2 sa ?status=: konverter prevodi enum u vrijednost iz baze (npr. UNPROCESSED -> neobradjen). */
	List<Problem> findByStatusOrderByVrijemeDescIdDesc(StatusProblema status);
}
