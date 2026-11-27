package org.etf.pumpd.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sigurnost (2.4.3), bez baze: nijedna ruta ne vraća entitet (npr. Nalog sa heš lozinkom) i nijedan
 * odgovor nema polje za lozinku, osim temporaryPassword (privremena lozinka koju radnik ili admin
 * jednom vidi pri kreiranju naloga, 11.3, 12.3, 12.7) i oznake mustChangePassword.
 */
class OdgovoriBezLozinkiTest {

    /** temporaryPassword: jednom se prikazuje pri kreiranju naloga; mustChangePassword je samo oznaka da/ne. */
    private static final Set<String> DOZVOLJENA_POLJA = Set.of("temporaryPassword", "mustChangePassword");

    private static List<Class<?>> kontroleri() throws ClassNotFoundException {
        var skener = new ClassPathScanningCandidateComponentProvider(false);
        skener.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        List<Class<?>> klase = new ArrayList<>();
        for (BeanDefinition definicija : skener.findCandidateComponents("org.etf.pumpd")) {
            klase.add(Class.forName(definicija.getBeanClassName()));
        }
        return klase;
    }

    /** Sve klase koje se pojavljuju u tipu odgovora (List<X>, ResponseEntity<X>, polja recorda...). */
    private static void skupi(Type tip, Set<Class<?>> klase) {
        if (tip instanceof ParameterizedType parametrizovan) {
            skupi(parametrizovan.getRawType(), klase);
            for (Type argument : parametrizovan.getActualTypeArguments()) {
                skupi(argument, klase);
            }
        } else if (tip instanceof Class<?> klasa && klasa.getName().startsWith("org.etf.pumpd") && klase.add(klasa)) {
            if (klasa.isRecord()) {
                for (RecordComponent polje : klasa.getRecordComponents()) {
                    skupi(polje.getGenericType(), klase);
                }
            }
        }
    }

    @Test
    void rutePronadjeneSkeniranjem() throws Exception {
        assertTrue(kontroleri().size() >= 20, "skener nije našao kontrolere");
    }

    @Test
    void nijedanOdgovorNijeEntitetNitiImaPoljeZaLozinku() throws Exception {
        List<String> greske = new ArrayList<>();
        int ruta = 0;
        for (Class<?> kontroler : kontroleri()) {
            for (Method metoda : kontroler.getDeclaredMethods()) {
                if (!AnnotatedElementUtils.hasAnnotation(metoda, RequestMapping.class)) {
                    continue;
                }
                ruta++;
                Set<Class<?>> klase = new HashSet<>();
                skupi(metoda.getGenericReturnType(), klase);
                for (Class<?> klasa : klase) {
                    String opis = kontroler.getSimpleName() + "." + metoda.getName() + " -> " + klasa.getSimpleName();
                    if (klasa.getPackageName().endsWith(".entity")) {
                        greske.add(opis + " je entitet");
                    }
                    if (klasa.isRecord()) {
                        for (RecordComponent polje : klasa.getRecordComponents()) {
                            String ime = polje.getName().toLowerCase();
                            if ((ime.contains("password") || ime.contains("lozink") || ime.contains("hash"))
                                    && !DOZVOLJENA_POLJA.contains(polje.getName())) {
                                greske.add(opis + "." + polje.getName());
                            }
                        }
                    }
                }
            }
        }
        assertTrue(ruta >= 69, "pronađeno samo " + ruta + " ruta");
        assertEquals(List.of(), greske, String.join("\n", greske));
    }
}
