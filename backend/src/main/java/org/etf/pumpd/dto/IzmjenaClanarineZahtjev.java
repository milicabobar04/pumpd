package org.etf.pumpd.dto;

import java.time.LocalDate;


import jakarta.validation.constraints.*;

public record IzmjenaClanarineZahtjev(@NotNull(message="Datum uplate je obavezan.") LocalDate paymentDate, 
		@NotNull(message="Datum isteka je obavezan.")LocalDate validUntil,
		@NotNull(message="Cijena je obavezna.")
		@PositiveOrZero(message="Cijena ne moze biti negativna.")
		@DecimalMax(value="100000",message="Cijena je prevelika.")Double price) {

}
