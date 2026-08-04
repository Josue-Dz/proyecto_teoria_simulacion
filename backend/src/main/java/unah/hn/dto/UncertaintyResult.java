package unah.hn.dto;

import java.util.List;
import java.util.Map;

public record UncertaintyResult(
        String id,
        int runs,
        double saturationProbability,
        Map<String, List<BandPoint>> bands,
        SimulationKpis medianKpis
) {}
