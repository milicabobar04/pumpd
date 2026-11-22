package org.etf.pumpd.controller;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import org.etf.pumpd.dto.SlikaOdgovor;
import org.etf.pumpd.exception.ResursNijePronadjenException;
import org.etf.pumpd.service.SlikeService;

/**
 * Slike objava. Slanje: samo TRAINER (SecurityConfig), multipart sa poljem "image".
 * Čitanje: javno, jer je ime nasumično (UUID) i do njega se dolazi samo preko zaštićenih ruta;
 * tako aplikacija sliku prikazuje običnim <Image uri>, bez tokena.
 */
@RestController
public class SlikeController {

	private final SlikeService slikeService;

	public SlikeController(SlikeService slikeService) {
		this.slikeService = slikeService;
	}

	@PostMapping("/api/posts/images")
	@ResponseStatus(HttpStatus.CREATED)
	public SlikaOdgovor posaljiSliku(@RequestParam("image") MultipartFile slika) {
		return new SlikaOdgovor(slikeService.sacuvaj(slika));
	}

	/** Sadržaj slike pod istim imenom se nikad ne mijenja, pa telefon smije dugo keširati. */
	@GetMapping("/api/images/{ime}")
	public ResponseEntity<Resource> slika(@PathVariable String ime) {
		Path fajl = slikeService.putanjaPoImenu(ime)
				.filter(Files::isRegularFile)
				.orElseThrow(() -> new ResursNijePronadjenException("Slika ne postoji."));
		return ResponseEntity.ok()
				.contentType(slikeService.tip(fajl))
				.cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic())
				.body(new FileSystemResource(fajl));
	}
}
