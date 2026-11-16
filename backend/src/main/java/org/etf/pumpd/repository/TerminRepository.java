package org.etf.pumpd.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.etf.pumpd.entity.Termin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TerminRepository extends JpaRepository<Termin,Integer> {

	@Query("SELECT t FROM Termin t JOIN FETCH t.grupa WHERE t.id = :id")
	Optional<Termin> saGrupom(@Param("id") Integer id);
	
	@Query("SELECT t FROM Termin t JOIN FETCH t.grupa g WHERE g.id = :idGrupe")
	Optional<Termin> poGrupi(@Param("idGrupe") Integer idGrupe);
	
	
	@Query("""
			SELECT t FROM Termin t JOIN FETCH t.grupa
			WHERE t.trener.id = :idTrenera
			  AND t.vrijeme >= :od AND t.vrijeme < :do
			ORDER BY t.vrijeme, t.id
			""")
	List<Termin> terminiTreneraIzmedju(@Param("idTrenera")Integer idTrenera,@Param("od")LocalDateTime od, @Param("do") LocalDateTime doVremena);
	
	@Query("""
			SELECT t FROM Termin t JOIN FETCH t.grupa
			WHERE t.trener.id = :idTrenera
			ORDER BY t.vrijeme DESC, t.id DESC
			""")
	List<Termin> terminiTrenera(@Param("idTrenera")Integer idTrenera);
	
	
}
