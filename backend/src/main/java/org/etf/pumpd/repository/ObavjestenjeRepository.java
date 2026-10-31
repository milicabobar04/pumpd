package org.etf.pumpd.repository;
import java.util.List;


import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import org.etf.pumpd.entity.*;

public interface ObavjestenjeRepository extends JpaRepository<Obavjestenje,Integer> {

	 List<Obavjestenje> findByPrimalacIdOrderByDatumVrijemeDescIdDesc(Integer primalacId);

	@Modifying
	@Query("update Obavjestenje o set o.procitano = true where o.primalac.id = :id and o.procitano = false")
    int oznaciSvaKaoProcitana(@Param("id") Integer primalacId);
}
