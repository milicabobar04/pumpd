package org.etf.pumpd.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TerminZahtjev(
		@NotBlank(message = "Naziv termina je obavezan.")
		@Size(max = 45, message = "Naziv termina moze imati najvise 45 znakova.")
		String name,
		@NotNull(message = "Vrijeme pocetka je obavezno.")
		LocalDateTime startTime,
		
		@NotNull(message = "Trajanje je obavezno.")
		@Min(value = 15, message = "Trajanje mora biti najmanje 15 minuta.")
		@Max(value = 300, message = "Trajanje moze biti najvise 300 minuta.")
		Integer durationMinutes,

		@NotNull(message = "Kapacitet je obavezan.")
		@Min(value = 1, message = "Kapacitet mora biti najmanje 1.")
		@Max(value = 100, message = "Kapacitet moze biti najvise 100.")
		Integer maxMembers

		)
{

}
