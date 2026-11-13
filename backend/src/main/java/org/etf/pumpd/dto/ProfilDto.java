package org.etf.pumpd.dto;

import org.etf.pumpd.enums.*;

/** 2.1 - mustChangePassword: isto kao u odgovoru prijave, za aplikaciju koja se pokrene sa sačuvanim tokenom. */
public record ProfilDto( Integer id,String name,String surname, String email, Integer birthYear, Double weight,Double height, Uloga role,
		boolean mustChangePassword) {

}
