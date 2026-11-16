package org.etf.pumpd.repository;

import java.util.Optional;

import org.etf.pumpd.entity.Grupa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface GrupaRepository extends JpaRepository<Grupa,Integer> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT g FROM Grupa g WHERE g.id = :id")
	Optional<Grupa> zakljucaj(@Param("id") Integer id);

}
