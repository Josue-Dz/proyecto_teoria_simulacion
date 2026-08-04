package unah.hn.service;

import unah.hn.dto.ScenarioDto;
import unah.hn.dto.SimulationRequest;
import unah.hn.dto.SimulationResult;
import unah.hn.model.ScenarioEntity;
import unah.hn.repository.ScenarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/** Gestión de escenarios guardados y su ejecución. */
@Service
@Transactional
public class ScenarioService {

    private final ScenarioRepository repository;
    private final ScenarioMapper mapper;
    private final DefaultsProvider defaults;
    private final SimulationService simulationService;

    public ScenarioService(ScenarioRepository repository, ScenarioMapper mapper,
                           DefaultsProvider defaults, SimulationService simulationService) {
        this.repository = repository;
        this.mapper = mapper;
        this.defaults = defaults;
        this.simulationService = simulationService;
    }

    /** Guarda un escenario nuevo (rellena parámetros faltantes con los valores por defecto). */
    public ScenarioDto create(ScenarioDto dto) {
        SimulationRequest req = defaults.normalize(dto != null ? dto.request() : null);
        String name = (dto != null && dto.name() != null && !dto.name().isBlank())
                ? dto.name() : "Escenario sin nombre";
        String description = dto != null ? dto.description() : null;
        ScenarioEntity saved = repository.saveAndFlush(mapper.toEntity(name, description, req));
        return mapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<ScenarioDto> list() {
        return repository.findAll().stream().map(mapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public ScenarioDto get(UUID id) {
        return mapper.toDto(load(id));
    }

    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new NoSuchElementException("No existe el escenario " + id);
        }
        repository.deleteById(id);
    }

    /** Ejecuta un escenario guardado y captura la corrida asociada a su id. */
    public SimulationResult run(UUID id) {
        ScenarioEntity entity = load(id);
        SimulationRequest req = mapper.toRequest(entity);
        return simulationService.runForScenario(req, id);
    }

    private ScenarioEntity load(UUID id) {
        return repository.findById(id).orElseThrow(
                () -> new NoSuchElementException("No existe el escenario " + id));
    }
}
