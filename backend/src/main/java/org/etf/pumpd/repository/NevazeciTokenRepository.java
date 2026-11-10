package org.etf.pumpd.repository;

import java.time.Instant;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import org.etf.pumpd.entity.*;

public interface NevazeciTokenRepository extends JpaRepository<NevazeciToken,String> {

	@Modifying
	@Query("delete from NevazeciToken t where t.istice < :sada")
	int obrisiIstekle(@Param("sada") Instant sada);
}
