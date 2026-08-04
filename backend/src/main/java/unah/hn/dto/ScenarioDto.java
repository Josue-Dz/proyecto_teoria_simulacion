package unah.hn.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

public record ScenarioDto(
        String id,
        @Size(max = 150, message = "el nombre no puede pasar de 150 caracteres")
        String name,
        String description,
        String createdAt,
        @Valid SimulationRequest request
) {}
