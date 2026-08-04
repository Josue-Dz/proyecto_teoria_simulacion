package unah.hn.dto;

import java.util.List;

public record SimulationResult(
        String id,
        List<DailyState> series,
        SimulationKpis kpis
) {}
