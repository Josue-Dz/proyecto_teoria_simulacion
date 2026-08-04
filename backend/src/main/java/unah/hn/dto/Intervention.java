package unah.hn.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record Intervention(
        @NotNull(message = "la intervención necesita un tipo")
        InterventionType type,

        @NotNull(message = "la intervención necesita un día de inicio")
        @Min(value = 0, message = "el día de inicio no puede ser negativo")
        Integer startDay,

        @NotNull(message = "la intervención necesita una magnitud")
        @PositiveOrZero(message = "la magnitud no puede ser negativa")
        Double magnitude
) {}
