package org.etf.pumpd.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PlanZahtjev(
		
		@NotBlank(message="Naziv plana je obavezan.")
		@Size(max=45,message="Naziv plana moze imati najvise 45 znakova") String name,
		@Size(max=500,message="Opis moze imati najvise 500 znakova") String description,
		@NotNull(message="Trajanje je obavezno.")
		@Min(value=1,message="Trajanje mora biti najmanje 1 minut.")
		@Max(value=600,message="Trajanje moze biti najvise 600 minuta.")
		Integer durationMinutes,
		
		@NotEmpty(message="Plan mora imati bar jednu vjezbu.")
		@Size(max=50,message="Plan moze imati najvise 50 vjezbi.")
		List<@Valid @NotNull(message="Vjezba ne moze biti prazna.") VjezbaZahtjev> exercises
		) {

}
