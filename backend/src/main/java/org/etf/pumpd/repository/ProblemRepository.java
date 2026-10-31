package org.etf.pumpd.repository;



import org.springframework.data.jpa.repository.*;

import org.etf.pumpd.entity.*;

public interface ProblemRepository extends JpaRepository<Problem,Integer> {

}
