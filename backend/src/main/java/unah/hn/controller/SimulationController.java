package unah.hn.controller;

import unah.hn.dto.CompareRequest;
import unah.hn.dto.CompareResult;
import unah.hn.dto.SimulationRequest;
import unah.hn.dto.SimulationResult;
import unah.hn.dto.UncertaintyRequest;
import unah.hn.dto.UncertaintyResult;
import unah.hn.service.SimulationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/simulations")
public class SimulationController {

    private final SimulationService service;

    public SimulationController(SimulationService service) {
        this.service = service;
    }

    /** Ejecuta una corrida determinista. */
    @PostMapping("/run")
    public SimulationResult run(@RequestBody(required = false) @Valid SimulationRequest request) {
        return service.run(request);
    }

    /** Ejecuta N corridas estocásticas y devuelve bandas de incertidumbre. */
    @PostMapping("/uncertainty")
    public UncertaintyResult uncertainty(@RequestBody(required = false) @Valid UncertaintyRequest request) {
        return service.uncertainty(request);
    }

    /** Compara escenarios. */
    @PostMapping("/compare")
    public CompareResult compare(@RequestBody @Valid CompareRequest request) {
        return service.compare(request);
    }

    /** Recupera una corrida ya ejecutada. */
    @GetMapping("/{id}")
    public SimulationResult get(@PathVariable String id) {
        return service.find(id);
    }

    /** Exporta una corrida a CSV */
    @GetMapping(value = "/{id}/export", produces = "text/csv")
    public ResponseEntity<String> export(@PathVariable String id) {
        String csv = service.exportCsv(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"denguesim-" + id + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
