package unah.hn.repository;

import org.junit.jupiter.api.Test;
import unah.hn.dto.SimulationResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** El almacen es una cache acotada: no puede crecer sin limite. */
class SimulationStoreTest {

    private SimulationResult corrida(String id) {
        return new SimulationResult(id, List.of(), null);
    }

    @Test
    void guardaYRecupera() {
        SimulationStore store = new SimulationStore();
        store.save(corrida("abc"));

        assertTrue(store.find("abc").isPresent());
        assertFalse(store.find("otro").isPresent());
    }

    @Test
    void noCreceMasAllaDeLaCapacidad() {
        SimulationStore store = new SimulationStore();
        for (int i = 0; i < SimulationStore.CAPACITY * 3; i++) {
            store.save(corrida("corrida-" + i));
        }

        assertEquals(SimulationStore.CAPACITY, store.size());
        assertFalse(store.find("corrida-0").isPresent(), "la mas antigua debio descartarse");
        assertTrue(store.find("corrida-" + (SimulationStore.CAPACITY * 3 - 1)).isPresent(),
                "la mas reciente debe seguir disponible");
    }

    /** Consultar una corrida la rejuvenece: se descarta la usada hace mas tiempo, no la mas vieja. */
    @Test
    void desalojaLaMenosUsadaRecientemente() {
        SimulationStore store = new SimulationStore();
        for (int i = 0; i < SimulationStore.CAPACITY; i++) {
            store.save(corrida("corrida-" + i));
        }

        store.find("corrida-0");                 // la volvemos a usar
        store.save(corrida("recien-llegada"));   // fuerza un desalojo

        assertTrue(store.find("corrida-0").isPresent(), "la recien consultada no debio salir");
        assertFalse(store.find("corrida-1").isPresent(), "debio salir la menos usada");
    }
}
