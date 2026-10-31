package org.etf.pumpd.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import org.etf.pumpd.entity.*;

public interface NalogRepository extends JpaRepository<Nalog,Integer> {

	Optional<Nalog> findByEmail(String email);
	boolean existsByEmail(String email);
	
	@Query(value= """
            select case
            when exists (select 1 from administrator a where a.zaposleni_nalog_idnalog = :id) then 'ADMIN'
            when exists (select 1 from radnik r where r.zaposleni_nalog_idnalog = :id) then 'WORKER'
            when exists (select 1 from trener t where t.nalog_idnalog = :id) then 'TRAINER'
            when exists (select 1 from clan c where c.nalog_idnalog = :id) then 'MEMBER'
        end
        """, nativeQuery=true) Optional<String> pronadjiUlogu(@Param("id") Integer id);
}
