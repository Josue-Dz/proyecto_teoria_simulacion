package unah.hn.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

public record HospitalParameters(
        // c0 : camas iniciales
        @DecimalMin(value = "0", message = "las camas iniciales no pueden ser negativas")
        @DecimalMax(value = "1000000", message = "las camas iniciales exceden el máximo admitido")
        Double initialBeds,

        // p_h : fracción de infecciosos que requiere cama
        @DecimalMin(value = "0", message = "la fracción hospitalizada debe estar entre 0 y 1")
        @DecimalMax(value = "1", message = "la fracción hospitalizada debe estar entre 0 y 1")
        Double hospitalizedFraction,

        // tau : retardo síntoma -> ingreso
        @DecimalMin(value = "0", message = "el retardo de ingreso no puede ser negativo")
        @DecimalMax(value = "365", message = "el retardo de ingreso no puede pasar de 365 días")
        Double admissionDelayDays,

        // LOS : estancia media (1/mu_s)
        @DecimalMin(value = "0", inclusive = false, message = "la estancia media debe ser mayor que 0")
        @DecimalMax(value = "365", message = "la estancia media no puede pasar de 365 días")
        Double avgStayDays,

        // umbral de espera tras el cual leve -> grave
        @DecimalMin(value = "0", inclusive = false, message = "el umbral de deterioro debe ser mayor que 0")
        @DecimalMax(value = "365", message = "el umbral de deterioro no puede pasar de 365 días")
        Double deteriorationThresholdDays,

        // mortalidad diaria de un grave que sigue en cola
        @DecimalMin(value = "0", message = "la mortalidad de grave debe estar entre 0 y 1")
        @DecimalMax(value = "1", message = "la mortalidad de grave debe estar entre 0 y 1")
        Double graveMortalityPerDay,

        // espera máxima antes de contarse como no atendido
        @DecimalMin(value = "0", inclusive = false, message = "la espera máxima debe ser mayor que 0")
        @DecimalMax(value = "365", message = "la espera máxima no puede pasar de 365 días")
        Double maxWaitDays,

        // fracción vulnerable: casos con riesgo real de muerte
        @DecimalMin(value = "0", message = "la fracción vulnerable debe estar entre 0 y 1")
        @DecimalMax(value = "1", message = "la fracción vulnerable debe estar entre 0 y 1")
        Double severeFraction
) {}
