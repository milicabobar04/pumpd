package org.etf.pumpd.dto;

import org.etf.pumpd.enums.*;

public record ProfilDto( Integer id,String name,String surname, String email, Integer birthYear, Double weight,Double height, Uloga role) {

}
