package unah.hn.dto;

public record SimulationKpis(
        double r0,
        Integer saturationDay,        // primer día con cola > 0 ; null si nunca satura
        boolean saturated,
        double peakPressureRatio,     // pico de (demanda / capacidad)
        Integer peakPressureDay,
        double maxBedDeficit,         // máxima cola (pacientes sin cama a la vez)
        double patientsNeverGotBed,   // acumulado que nunca recibió cama
        double deathsFromSaturation,  // muertes atribuibles a la falta de cama
        double avgWaitDays,           // espera media de los que sí ingresaron por cola
        double finalAttackRate,       // (N_h - S_h_final) / N_h
        double totalHospitalizations  // total de casos que requirieron cama
) {}
