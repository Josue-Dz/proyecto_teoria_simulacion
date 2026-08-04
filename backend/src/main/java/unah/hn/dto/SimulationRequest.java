package unah.hn.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.List;

public record SimulationRequest(
        @Valid ModelParameters model,
        @Valid HospitalParameters hospital,
        @Valid List<Intervention> interventions,

        // horizonte de simulación (por defecto 365)
        @Min(value = 1, message = "el horizonte debe ser de al menos 1 día")
        @Max(value = 3650, message = "el horizonte no puede pasar de 3650 días")
        Integer horizonDays,

        Long seed,             // semilla para el modo estocástico
        Boolean stochastic     // si true, usa el motor de cola por eventos discretos (DES)
) {}
