package unah.hn.dto;

import java.util.List;

public record CompareResult(List<NamedResult> results) {
    public record NamedResult(String name, SimulationResult result) {}
}
