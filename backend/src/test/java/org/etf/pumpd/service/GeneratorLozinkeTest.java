package org.etf.pumpd.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

class GeneratorLozinkeTest {

    private final GeneratorLozinke generator = new GeneratorLozinke();

    @RepeatedTest(200)
    void lozinkaImaDvanaestZnakovaIIzSvakeGrupeBarJedan() {
        String lozinka = generator.generisi();

        assertEquals(12, lozinka.length());
        assertTrue(lozinka.chars().anyMatch(c -> GeneratorLozinke.VELIKA.indexOf(c) >= 0), lozinka);
        assertTrue(lozinka.chars().anyMatch(c -> GeneratorLozinke.MALA.indexOf(c) >= 0), lozinka);
        assertTrue(lozinka.chars().anyMatch(c -> GeneratorLozinke.CIFRE.indexOf(c) >= 0), lozinka);
    }

    @RepeatedTest(200)
    void bezZnakovaKojiSeLakoPobrkaju() {
        String lozinka = generator.generisi();

        for (char zabranjen : "0Oo1lI".toCharArray()) {
            assertTrue(lozinka.indexOf(zabranjen) < 0, lozinka);
        }
    }

    @Test
    void lozinkeSuRazlicite() {
        Set<String> lozinke = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            lozinke.add(generator.generisi());
        }
        assertEquals(1000, lozinke.size());
    }
}
