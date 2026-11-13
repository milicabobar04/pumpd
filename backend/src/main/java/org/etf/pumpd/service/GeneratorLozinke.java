package org.etf.pumpd.service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

/**
 * Privremena lozinka za nalog koji pravi radnik ili administrator (11.3, 12.3, 12.7).
 * 12 znakova, uvijek bar jedno veliko slovo, jedno malo i jedna cifra, bez znakova
 * koji se lako pobrkaju pri prepisivanju (0 O o 1 l I).
 */
@Component
public class GeneratorLozinke {

	static final int DUZINA = 12;
	static final String VELIKA = "ABCDEFGHJKLMNPQRSTUVWXYZ";
	static final String MALA = "abcdefghijkmnpqrstuvwxyz";
	static final String CIFRE = "23456789";
	private static final String SVI = VELIKA + MALA + CIFRE;

	private final SecureRandom slucajno = new SecureRandom();

	public String generisi() {
		List<Character> znakovi = new ArrayList<>(DUZINA);
		znakovi.add(nasumican(VELIKA));
		znakovi.add(nasumican(MALA));
		znakovi.add(nasumican(CIFRE));
		while (znakovi.size() < DUZINA) {
			znakovi.add(nasumican(SVI));
		}
		Collections.shuffle(znakovi, slucajno);

		StringBuilder lozinka = new StringBuilder(DUZINA);
		znakovi.forEach(lozinka::append);
		return lozinka.toString();
	}

	private char nasumican(String skup) {
		return skup.charAt(slucajno.nextInt(skup.length()));
	}
}
