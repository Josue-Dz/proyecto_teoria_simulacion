package unah.hn.controller;

import unah.hn.dto.RunSummaryDto;
import unah.hn.dto.SimulationKpis;
import unah.hn.model.SimulationRunEntity;
import unah.hn.repository.SimulationRunRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/runs")
public class RunController {

    private final SimulationRunRepository repository;

    public RunController(SimulationRunRepository repository) {
        this.repository = repository;
    }

    /** Todas las corridas, más recientes primero. */
    @GetMapping
    public List<RunSummaryDto> history() {
        return repository.findAllByOrderByCreatedAtDesc().stream().map(this::toDto).toList();
    }

    /** Corridas de un escenario concreto. */
    @GetMapping("/scenario/{scenarioId}")
    public List<RunSummaryDto> byScenario(@PathVariable UUID scenarioId) {
        return repository.findByScenarioIdOrderByCreatedAtDesc(scenarioId).stream().map(this::toDto).toList();
    }

    private RunSummaryDto toDto(SimulationRunEntity e) {
        SimulationKpis kpis = new SimulationKpis(
                nz(e.getR0()), e.getSaturationDay(), Boolean.TRUE.equals(e.getSaturated()),
                nz(e.getPeakPressureRatio()), e.getPeakPressureDay(), nz(e.getMaxBedDeficit()),
                nz(e.getPatientsNeverGotBed()), nz(e.getDeathsFromSaturation()),
                nz(e.getAvgWaitDays()), nz(e.getFinalAttackRate()), nz(e.getTotalHospitalizations()));
        return new RunSummaryDto(
                e.getId() != null ? e.getId().toString() : null,
                e.getScenarioId() != null ? e.getScenarioId().toString() : null,
                e.getCreatedAt() != null ? e.getCreatedAt().toString() : null,
                e.getSeed(), Boolean.TRUE.equals(e.getStochastic()), kpis);
    }

    private double nz(Double v) {
        return v != null ? v : 0.0;
    }
}
