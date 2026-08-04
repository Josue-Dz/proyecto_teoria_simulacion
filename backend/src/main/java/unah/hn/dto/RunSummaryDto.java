package unah.hn.dto;

public record RunSummaryDto(
        String id,
        String scenarioId,
        String createdAt,
        Long seed,
        boolean stochastic,
        SimulationKpis kpis
) {}
