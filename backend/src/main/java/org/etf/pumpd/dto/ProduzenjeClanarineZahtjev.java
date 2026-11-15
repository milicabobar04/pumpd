package org.etf.pumpd.dto;


import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ProduzenjeClanarineZahtjev(@NotNull(message="Broj mjeseci je obavezan.") 
		@Min(value=1,message="Broj mjeseci mora biti najmanje 1.")
		@Max(value=24,message="Broj mjeseci moze biti najvise 24.") Integer numMonths) {

}
