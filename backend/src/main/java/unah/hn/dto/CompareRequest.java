package unah.hn.dto;

import jakarta.validation.Valid;

import java.util.List;

public record CompareRequest(@Valid List<NamedScenario> scenarios) {
    public record NamedScenario(String name, @Valid SimulationRequest request) {}
}
