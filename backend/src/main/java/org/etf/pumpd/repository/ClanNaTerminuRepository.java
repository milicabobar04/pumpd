package org.etf.pumpd.repository;

import java.util.Collection;
import java.util.Optional;

import org.etf.pumpd.entity.*;
import org.etf.pumpd.enums.StatusPrijave;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface ClanNaTerminuRepository extends JpaRepository<ClanNaTerminu,ClanNaTerminuId>{

	Optional<ClanNaTerminu> findByIdZahtjeva(Integer idZahtjeva);
	
	@Query("""
			SELECT p FROM ClanNaTerminu p
			WHERE p.termin.id = :idTermina AND p.status IN :statusi
			""")
	List<ClanNaTerminu> prijaveNaTermin(@Param("idTermina")Integer idTermina,@Param("statusi") Collection<StatusPrijave> statusi);
	
	@Modifying
	@Query("DELETE FROM ClanNaTerminu p WHERE p.termin.id = :idTermina")
	int obrisiSveZaTermin(@Param("idTermina") Integer idTermina);

	/** Id-jevi članova čija prijava na termin ima jedan od datih statusa, bez učitavanja prijava (10.7). */
	@Query("SELECT p.id.clanId FROM ClanNaTerminu p WHERE p.termin.id = :idTermina AND p.status IN :statusi")
	List<Integer> idClanova(@Param("idTermina") Integer idTermina, @Param("statusi") Collection<StatusPrijave> statusi);
}
