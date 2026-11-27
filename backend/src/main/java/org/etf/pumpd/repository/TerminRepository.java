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
	
	

	/**
	 * 9.1: termini aktivnih trenera od "od" (uključivo) do "doVremena" (isključivo, null = bez kraja),
	 * opciono samo jednog trenera. Grupa, trener i nalog se učitavaju odmah (naziv, ime i kapacitet).
	 */
	@Query("""
			SELECT t FROM Termin t JOIN FETCH t.grupa JOIN FETCH t.trener tr JOIN FETCH tr.nalog n
			WHERE n.aktivan = true
			  AND t.vrijeme >= :od
			  AND (:do IS NULL OR t.vrijeme < :do)
			  AND (:idTrenera IS NULL OR tr.id = :idTrenera)
			ORDER BY t.vrijeme, t.id
			""")
	List<Termin> pregledTermina(@Param("od") LocalDateTime od, @Param("do") LocalDateTime doVremena,
			@Param("idTrenera") Integer idTrenera);

	/** 9.2, 9.3: termin sa grupom, trenerom i njegovim nalogom (ime za obavještenje, provjera aktivnosti). */
	@Query("SELECT t FROM Termin t JOIN FETCH t.grupa JOIN FETCH t.trener tr JOIN FETCH tr.nalog WHERE t.id = :id")
	Optional<Termin> saGrupomITrenerom(@Param("id") Integer id);
}
