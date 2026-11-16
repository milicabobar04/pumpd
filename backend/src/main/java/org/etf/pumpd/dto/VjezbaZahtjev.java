package org.etf.pumpd.dto;

import jakarta.validation.constraints.*;

public record VjezbaZahtjev(
		Integer exerciseId,
		@NotBlank(message="Naziv vjezbe je obavezan.")
		@Size(max=45,message="Naziv vjezbe moze imati najvise 45 znakova") String name,
		@NotNull(message="Broj serija je obavezan.")
		@Min(value=1,message="Broj serija mora biti najmanje 1.")
		@Max(value=100,message="Broj serija moze biti najvise 100.")
		Integer numSets,
		
		@NotNull(message="Broj ponavljanja je obavezan.")
		@Min(value=1,message="Broj ponavljanja mora biti najmanje 1.")
		@Max(value=1000,message="Broj ponavljanja moze biti najvise 1000.")
		Integer numRepetitions
		
		) {

}
