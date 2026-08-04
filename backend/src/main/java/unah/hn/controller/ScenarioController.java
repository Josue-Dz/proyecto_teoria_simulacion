package unah.hn.controller;

import unah.hn.dto.ScenarioDto;
import unah.hn.dto.SimulationResult;
import unah.hn.service.ScenarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/scenarios")
public class ScenarioController {

    private final ScenarioService service;

    public ScenarioController(ScenarioService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ScenarioDto create(@RequestBody @Valid ScenarioDto dto) {
        return service.create(dto);
    }

    @GetMapping
    public List<ScenarioDto> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public ScenarioDto get(@PathVariable UUID id) {
        return service.get(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    /** Ejecuta el escenario guardado y captura la corrida. */
    @PostMapping("/{id}/run")
    public SimulationResult run(@PathVariable UUID id) {
        return service.run(id);
    }
}
