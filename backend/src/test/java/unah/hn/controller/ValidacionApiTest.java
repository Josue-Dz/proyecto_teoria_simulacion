package unah.hn.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** La API debe rechazar parametros imposibles con un 400 explicativo, no con un 500. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ValidacionApiTest {

    @Autowired
    private MockMvc mockMvc;

    private org.springframework.test.web.servlet.ResultActions ejecutar(String cuerpo) throws Exception {
        return mockMvc.perform(post("/api/simulations/run")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo));
    }

    /** Una peticion vacia es valida: el servidor rellena con los valores por defecto. */
    @Test
    void peticionVaciaEsValida() throws Exception {
        ejecutar("{}").andExpect(status().isOk());
    }

    /** Enviar solo algunos parametros sigue siendo valido. */
    @Test
    void peticionParcialEsValida() throws Exception {
        ejecutar("{\"hospital\":{\"initialBeds\":800}}").andExpect(status().isOk());
    }

    @Test
    void rechazaProbabilidadMayorQueUno() throws Exception {
        ejecutar("{\"model\":{\"betaHuman\":1.5}}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Parámetros inválidos"))
                .andExpect(jsonPath("$.fields['model.betaHuman']").exists());
    }

    @Test
    void rechazaCamasNegativas() throws Exception {
        ejecutar("{\"hospital\":{\"initialBeds\":-10}}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields['hospital.initialBeds']").exists());
    }

    @Test
    void rechazaEstanciaMediaCero() throws Exception {
        ejecutar("{\"hospital\":{\"avgStayDays\":0}}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields['hospital.avgStayDays']").exists());
    }

    @Test
    void rechazaHorizonteDesmedido() throws Exception {
        ejecutar("{\"horizonDays\":99999}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.horizonDays").exists());
    }

    @Test
    void rechazaIntervencionIncompleta() throws Exception {
        ejecutar("{\"interventions\":[{\"type\":\"FUMIGATION\"}]}")
                .andExpect(status().isBadRequest());
    }

    /** Relacion entre campos: no puede haber mas infectados que habitantes. */
    @Test
    void rechazaMasInfectadosQueHabitantes() throws Exception {
        ejecutar("{\"model\":{\"populationHuman\":1000,\"initialInfectedHumans\":5000}}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("infectados iniciales")));
    }

    /** Relacion entre campos: la espera maxima tiene que superar al umbral de deterioro. */
    @Test
    void rechazaEsperaMaximaMenorQueElUmbral() throws Exception {
        ejecutar("{\"hospital\":{\"deteriorationThresholdDays\":10,\"maxWaitDays\":3}}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("espera máxima")));
    }

    /** Un cuerpo que no es JSON valido devuelve 400, no 500. */
    @Test
    void rechazaJsonMalFormado() throws Exception {
        ejecutar("{esto no es json}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Petición ilegible"));
    }

    @Test
    void rechazaDemasiadasCorridasDeIncertidumbre() throws Exception {
        mockMvc.perform(post("/api/simulations/uncertainty")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"runs\":50000}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.runs").exists());
    }
}
