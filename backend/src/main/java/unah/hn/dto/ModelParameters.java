package unah.hn.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

public record ModelParameters(
        // N_h : población humana total
        @DecimalMin(value = "1", message = "la población humana debe ser de al menos 1 persona")
        @DecimalMax(value = "1000000000", message = "la población humana excede el máximo admitido")
        Double populationHuman,

        // m : N_v = m * N_h (mosquitos por humano)
        @DecimalMin(value = "0", message = "los mosquitos por humano no pueden ser negativos")
        @DecimalMax(value = "100", message = "los mosquitos por humano no pueden pasar de 100")
        Double vectorRatio,

        // b : picaduras por mosquito por día
        @DecimalMin(value = "0", message = "la tasa de picadura no puede ser negativa")
        @DecimalMax(value = "10", message = "la tasa de picadura no puede pasar de 10 por día")
        Double bitingRate,

        // beta_h : prob. transmisión vector -> humano
        @DecimalMin(value = "0", message = "la transmisión vector-humano debe estar entre 0 y 1")
        @DecimalMax(value = "1", message = "la transmisión vector-humano debe estar entre 0 y 1")
        Double betaHuman,

        // beta_v : prob. transmisión humano -> vector
        @DecimalMin(value = "0", message = "la transmisión humano-vector debe estar entre 0 y 1")
        @DecimalMax(value = "1", message = "la transmisión humano-vector debe estar entre 0 y 1")
        Double betaVector,

        // 1/nu_h : incubación intrínseca
        @DecimalMin(value = "0", inclusive = false, message = "la incubación humana debe ser mayor que 0")
        @DecimalMax(value = "365", message = "la incubación humana no puede pasar de 365 días")
        Double incubationHumanDays,

        // 1/nu_v : incubación extrínseca (EIP)
        @DecimalMin(value = "0", inclusive = false, message = "la incubación en el mosquito debe ser mayor que 0")
        @DecimalMax(value = "365", message = "la incubación en el mosquito no puede pasar de 365 días")
        Double incubationVectorDays,

        // 1/gamma_h : periodo infeccioso humano
        @DecimalMin(value = "0", inclusive = false, message = "el periodo infeccioso debe ser mayor que 0")
        @DecimalMax(value = "365", message = "el periodo infeccioso no puede pasar de 365 días")
        Double infectiousHumanDays,

        // 1/mu_h : esperanza de vida humana
        @DecimalMin(value = "1", message = "la esperanza de vida debe ser de al menos 1 día")
        @DecimalMax(value = "100000", message = "la esperanza de vida excede el máximo admitido")
        Double lifeExpectancyDaysHuman,

        // 1/mu_v : vida media del mosquito
        @DecimalMin(value = "0", inclusive = false, message = "la vida del mosquito debe ser mayor que 0")
        @DecimalMax(value = "365", message = "la vida del mosquito no puede pasar de 365 días")
        Double mosquitoLifespanDays,

        // I_h(0) : semilla del brote
        @DecimalMin(value = "0", message = "los infectados iniciales no pueden ser negativos")
        @DecimalMax(value = "1000000000", message = "los infectados iniciales exceden el máximo admitido")
        Double initialInfectedHumans,

        // sigma : inmunidad previa al serotipo circulante. Va a R_h(0), así que
        //   S_h(0) = N_h·(1 - sigma). Sin ella el modelo arrancaría con toda la
        //   población susceptible, irreal en zona endémica. Introduce el umbral de
        //   inmunidad de grupo: el brote solo despega si sigma < 1 - 1/R_0².
        @DecimalMin(value = "0", message = "la inmunidad previa debe estar entre 0 y 1")
        @DecimalMax(value = "1", message = "la inmunidad previa debe estar entre 0 y 1")
        Double initialImmuneFraction
) {}
