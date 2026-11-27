package org.etf.pumpd.repository;

import org.etf.pumpd.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.*;
import java.util.*;

public interface ClanUGrupiRepository extends JpaRepository<ClanUGrupi,ClanUGrupiId>{

	@Query("SELECT COUNT(cg) FROM ClanUGrupi cg WHERE cg.grupa.id = :idGrupe")
	long brojClanova(@Param("idGrupe") Integer idGrupe);
	
	@Query("""
			SELECT cg FROM ClanUGrupi cg JOIN FETCH cg.clan c JOIN FETCH c.nalog n
			WHERE cg.grupa.id = :idGrupe AND n.aktivan = true
			ORDER BY n.prezime, n.ime
			""")
	List<ClanUGrupi> clanoviGrupe(@Param("idGrupe")Integer idGrupe);
	
	@Query("SELECT cg.id.clanId FROM ClanUGrupi cg WHERE cg.grupa.id = :idGrupe")
	List<Integer> idClanova(@Param("idGrupe")Integer idGrupe);
	
	@Modifying
	@Query("DELETE FROM ClanUGrupi cg WHERE cg.grupa.id = :idGrupe")
	int obrisiSveIzGrupe(@Param("idGrupe")Integer idGrupe);
	

	/** 9.1: broj članova po grupi za više grupa jednim upitom; red = [id grupe, broj]. Grupa bez članova nema red. */
	@Query("SELECT cg.grupa.id, COUNT(cg) FROM ClanUGrupi cg WHERE cg.grupa.id IN :idGrupa GROUP BY cg.grupa.id")
	List<Object[]> brojClanovaPoGrupama(@Param("idGrupa") Collection<Integer> idGrupa);
}
