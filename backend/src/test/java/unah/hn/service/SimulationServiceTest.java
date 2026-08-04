package unah.hn.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import unah.hn.dto.DailyState;
import unah.hn.dto.HospitalParameters;
import unah.hn.dto.SimulationRequest;
import unah.hn.dto.SimulationResult;
import unah.hn.dto.UncertaintyRequest;
import unah.hn.dto.UncertaintyResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pruebas del pipeline completo: epidemia -> hospital -> KPIs. */
@SpringBootTest
@ActiveProfiles("test")
class SimulationServiceTest {

    @Autowired
    private SimulationService service;

    /** Una peticion vacia debe rellenarse con los valores por defecto y correr igual. */
    @Test
    void corridaPorDefectoProduceSerieCompleta() {
        SimulationResult result = service.run(null);

        assertEquals(366, result.series().size(), "365 dias de horizonte + el dia 0");
        assertEquals(0, result.series().get(0).day());
        assertEquals(365, result.series().get(365).day());
        assertTrue(result.kpis().r0() > 1.0, "los parametros por defecto describen un brote que despega");
    }

    /** Con los parametros por defecto (150 camas, 1.3M habitantes) el sistema colapsa. */
    @Test
    void escenarioPorDefectoSaturaElHospital() {
        SimulationResult result = service.run(null);

        assertTrue(result.kpis().saturated(), "el brote por defecto debe desbordar las camas");
        assertNotNull(result.kpis().saturationDay());
        assertTrue(result.kpis().peakPressureRatio() > 1.0);
        assertTrue(result.kpis().deathsFromSaturation() > 0.0);
    }

    /** La poblacion humana total se conserva: el modelo no crea ni pierde personas. */
    @Test
    void laPoblacionHumanaSeConserva() {
        SimulationResult result = service.run(null);
        double poblacion = 1_342_329.0;

        for (DailyState d : result.series()) {
            double total = d.sh() + d.eh() + d.ih() + d.rh();
            assertEquals(poblacion, total, poblacion * 0.01,
                    "la suma S+E+I+R se desvia mas del 1% en el dia " + d.day());
        }
    }

    /** Misma semilla, misma corrida: el modo estocastico es reproducible. */
    @Test
    void mismaSemillaProduceMismaCorrida() {
        SimulationRequest req = new SimulationRequest(null, null, null, 120, 12345L, Boolean.TRUE);

        SimulationResult a = service.run(req);
        SimulationResult b = service.run(req);

        assertEquals(a.kpis().deathsFromSaturation(), b.kpis().deathsFromSaturation());
        assertEquals(a.kpis().maxBedDeficit(), b.kpis().maxBedDeficit());
        for (int t = 0; t < a.series().size(); t++) {
            assertEquals(a.series().get(t).bedsOccupied(), b.series().get(t).bedsOccupied(),
                    "divergen en el dia " + t);
        }
    }

    /** Las bandas de incertidumbre deben venir ordenadas p05 <= p50 <= p95. */
    @Test
    void bandasDeIncertidumbreEstanOrdenadas() {
        SimulationRequest base = new SimulationRequest(null, null, null, 90, 7L, Boolean.TRUE);
        UncertaintyResult result = service.uncertainty(new UncertaintyRequest(base, 10, 0.10));

        assertTrue(result.bands().containsKey("bedsOccupied"));
        result.bands().forEach((variable, puntos) -> {
            assertEquals(91, puntos.size(), "faltan puntos en la banda de " + variable);
            puntos.forEach(p -> assertTrue(p.p05() <= p.p50() && p.p50() <= p.p95(),
                    "percentiles desordenados en " + variable + " dia " + p.day()));
        });
    }

    /** El analisis de Monte Carlo reporta cuantas corridas hizo y su P(saturacion). */
    @Test
    void reportaProbabilidadDeSaturacion() {
        // Capacidad deliberadamente insuficiente (50 camas) y horizonte completo: asi la
        // saturacion es inequivoca y el test comprueba el mecanismo, no la calibracion.
        // Con los valores por defecto el sistema opera cerca del umbral a proposito, y
        // ahi la P(saturacion) del motor estocastico es sensible a la semilla.
        HospitalParameters pocasCamas =
                new HospitalParameters(50.0, null, null, null, null, null, null, null);
        SimulationRequest base = new SimulationRequest(null, pocasCamas, null, 365, 7L, Boolean.TRUE);
        UncertaintyResult result = service.uncertainty(new UncertaintyRequest(base, 12, 0.10));

        assertEquals(12, result.runs());
        assertTrue(result.saturationProbability() >= 0.0 && result.saturationProbability() <= 1.0);
        assertNotNull(result.medianKpis());
        assertEquals(1.0, result.saturationProbability(), 1e-9);
        assertTrue(result.medianKpis().saturated());
    }

    /**
     * El motor DES es una simulacion terminante: nada puede ocurrir despues del
     * horizonte, asi que ninguna espera puede superarlo.
     */
    @Test
    void elMotorEstocasticoNoSimulaMasAllaDelHorizonte() {
        int horizonte = 200;
        SimulationRequest req = new SimulationRequest(null, null, null, horizonte, 3L, Boolean.TRUE);

        SimulationResult result = service.run(req);

        assertTrue(result.kpis().avgWaitDays() <= horizonte,
                "espera media de " + result.kpis().avgWaitDays() + " dias en un horizonte de " + horizonte);
        double ingresosUltimoDia = result.series().get(horizonte).admissions();
        double ingresosPenultimo = result.series().get(horizonte - 1).admissions();
        assertTrue(ingresosUltimoDia <= Math.max(10, ingresosPenultimo * 3),
                "el ultimo dia acumula ingresos posteriores al horizonte: " + ingresosUltimoDia);
    }

    /** Los parametros fuera de rango se rechazan antes de simular. */
    @Test
    void rechazaHorizonteInvalido() {
        SimulationRequest req = new SimulationRequest(null, null, null, 0, null, Boolean.FALSE);
        assertThrows(IllegalArgumentException.class, () -> service.run(req));
    }
}
