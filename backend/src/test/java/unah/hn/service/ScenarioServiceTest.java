package unah.hn.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import unah.hn.dto.Intervention;
import unah.hn.dto.InterventionType;
import unah.hn.dto.ScenarioDto;
import unah.hn.dto.SimulationRequest;
import unah.hn.dto.SimulationResult;
import unah.hn.repository.SimulationRunRepository;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Ida y vuelta de un escenario contra la base de datos. */
@SpringBootTest
@ActiveProfiles("test")
class ScenarioServiceTest {

    @Autowired
    private ScenarioService service;

    @Autowired
    private SimulationRunRepository runRepository;

    /** Guardar y releer un escenario debe devolver exactamente los mismos parametros. */
    @Test
    void guardaYRecuperaEscenarioConIntervenciones() {
        SimulationRequest peticion = new SimulationRequest(
                null, null,
                List.of(new Intervention(InterventionType.FUMIGATION, 40, 0.6),
                        new Intervention(InterventionType.EXTRA_BEDS, 60, 200.0)),
                180, 99L, Boolean.TRUE);

        ScenarioDto guardado = service.create(
                new ScenarioDto(null, "Fumigacion temprana", "prueba", null, peticion));

        assertNotNull(guardado.id());
        assertNotNull(guardado.createdAt());

        ScenarioDto leido = service.get(UUID.fromString(guardado.id()));
        assertEquals("Fumigacion temprana", leido.name());
        assertEquals(180, leido.request().horizonDays());
        assertEquals(99L, leido.request().seed());
        assertEquals(Boolean.TRUE, leido.request().stochastic());
        assertEquals(2, leido.request().interventions().size());
        // Los parametros omitidos se persisten ya rellenos con los valores por defecto.
        assertEquals(1_342_329.0, leido.request().model().populationHuman());
        assertEquals(150.0, leido.request().hospital().initialBeds());
    }

    /** Ejecutar un escenario guardado deja rastro en el historial de corridas. */
    @Test
    void ejecutarEscenarioRegistraLaCorrida() {
        ScenarioDto guardado = service.create(new ScenarioDto(null, "Linea base", null, null,
                new SimulationRequest(null, null, null, 120, null, Boolean.FALSE)));
        UUID id = UUID.fromString(guardado.id());

        SimulationResult resultado = service.run(id);

        assertEquals(121, resultado.series().size());
        assertFalse(runRepository.findByScenarioIdOrderByCreatedAtDesc(id).isEmpty(),
                "la corrida debe quedar capturada en simulation_run");
    }

    /** Borrar arrastra las intervenciones y deja el escenario inaccesible. */
    @Test
    void borraEscenario() {
        ScenarioDto guardado = service.create(new ScenarioDto(null, "Temporal", null, null,
                new SimulationRequest(null, null,
                        List.of(new Intervention(InterventionType.EXTRA_BEDS, 10, 50.0)),
                        90, null, Boolean.FALSE)));
        UUID id = UUID.fromString(guardado.id());

        service.delete(id);

        assertThrows(NoSuchElementException.class, () -> service.get(id));
        assertTrue(service.list().stream().noneMatch(s -> s.id().equals(guardado.id())));
    }

    /** Un id inexistente devuelve 404, no un 500. */
    @Test
    void escenarioInexistenteLanzaNoSuchElement() {
        assertThrows(NoSuchElementException.class, () -> service.get(UUID.randomUUID()));
    }
}
