package org.etf.pumpd.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import org.etf.pumpd.entity.*;

public interface ClanRepository extends JpaRepository<Clan,Integer> {

}
