package unah.hn.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record UncertaintyRequest(
        @Valid SimulationRequest base,
        @Min(value = 2, message = "hacen falta al menos 2 corridas para tener una banda")
        @Max(value = 1000, message = "el máximo son 1000 corridas")
        Integer runs,
        @DecimalMin(value = "0", message = "la variabilidad de la transmisión no puede ser negativa")
        @DecimalMax(value = "1", message = "la variabilidad de la transmisión no puede pasar de 1")
        Double transmissionVariability
) {}
