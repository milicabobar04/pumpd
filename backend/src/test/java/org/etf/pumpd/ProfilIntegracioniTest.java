package org.etf.pumpd;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.jayway.jsonpath.JsonPath;

/**
 * Integracioni test izmjene profila (2.2) nad pravom bazom: uspješna izmjena i čitanje (2.1).
 * Uz to, odgovori se provjeravaju prema openapi.yaml (OpenApiProvjera), pa je i 2.2 pokrivena.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(MySqlTestKonfiguracija.class)
@Testcontainers(disabledWithoutDocker = true)
class ProfilIntegracioniTest {

    @Autowired
    private MockMvc mockMvc;

    private String prijava(String email, String lozinka) throws Exception {
        String odgovor = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, lozinka)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(odgovor, "$.token");
    }

    @Test
    void clanMijenjaSvojProfil() throws Exception {
        String radnik = prijava("radnik@pumpd.local", "pumpd123");
        String email = "profil." + System.nanoTime() + "@mail.com";
        String novi = mockMvc.perform(post("/api/worker/members").header(HttpHeaders.AUTHORIZATION, "Bearer " + radnik)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Staro\",\"surname\":\"Ime\",\"email\":\"%s\"}".formatted(email)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String token = prijava(email, JsonPath.read(novi, "$.temporaryPassword"));

        mockMvc.perform(put("/api/users/my-profile").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Novo\",\"surname\":\"Prezime\",\"birthYear\":1999,\"weight\":72.5,\"height\":178}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Profil je uspješno ažuriran."));

        mockMvc.perform(get("/api/users/my-profile").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Novo"))
                .andExpect(jsonPath("$.surname").value("Prezime"))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.birthYear").value(1999))
                .andExpect(jsonPath("$.weight").value(72.5))
                .andExpect(jsonPath("$.height").value(178.0));
    }
}
