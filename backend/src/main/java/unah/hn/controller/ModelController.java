package unah.hn.controller;

import unah.hn.dto.SimulationRequest;
import unah.hn.service.DefaultsProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/model")
public class ModelController {

    private final DefaultsProvider defaults;

    public ModelController(DefaultsProvider defaults) {
        this.defaults = defaults;
    }

    /** Parámetros por defecto (petición base lista para editar y ejecutar). */
    @GetMapping("/defaults")
    public SimulationRequest defaults() {
        return defaults.defaultRequest();
    }
}
