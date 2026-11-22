package org.etf.pumpd.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.etf.pumpd.entity.SacuvanaObjava;
import org.etf.pumpd.entity.SacuvanaObjavaId;

public interface SacuvanaObjavaRepository extends JpaRepository<SacuvanaObjava, SacuvanaObjavaId> {

	/** 8.7: briše objavu iz sačuvanih kod svih članova, jednim upitom (prije brisanja objave). */
	@Modifying
	@Query("DELETE FROM SacuvanaObjava s WHERE s.objava.id = :idObjave")
	int obrisiZaObjavu(@Param("idObjave") Integer idObjave);

	/** 8.1: koje od zadatih objava je član sačuvao (za isSaved), jednim upitom za cijelu listu. */
	@Query("SELECT s.id.objavaId FROM SacuvanaObjava s WHERE s.id.clanId = :idClana AND s.id.objavaId IN :idObjava")
	List<Integer> sacuvaneMedju(@Param("idClana") Integer idClana, @Param("idObjava") Collection<Integer> idObjava);

	/**
	 * 8.3: sačuvane objave člana sa objavom, trenerom i nalogom (jedan upit), bez objava obrisanih
	 * trenera. datumCuvanja je samo datum, pa se za isti dan redaju po vremenu objave.
	 */
	@Query("""
			SELECT s FROM SacuvanaObjava s JOIN FETCH s.objava o JOIN FETCH o.trener t JOIN FETCH t.nalog n
			WHERE s.id.clanId = :idClana AND n.aktivan = true
			ORDER BY s.datumCuvanja DESC, o.vrijeme DESC, o.id DESC
			""")
	List<SacuvanaObjava> sacuvaneClana(@Param("idClana") Integer idClana);
}
