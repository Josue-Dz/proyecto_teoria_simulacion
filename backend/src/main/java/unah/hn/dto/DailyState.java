package unah.hn.dto;

public record DailyState(
        int day,
        // --- compartimentos SEIR-SEI ---
        double sh, double eh, double ih, double rh,
        double sv, double ev, double iv,
        double newCases,        // nuevos infecciosos humanos ese día (incidencia)
        // --- bloque hospitalario ---
        double bedsOccupied,
        double queueLength,
        double capacity,
        double admissions,
        double discharges,
        double deaths,          // muertes en cola ese día (por saturación)
        double cumulativeDeaths,
        double leftWithoutBed,  // salieron de la cola ese día sin haber recibido cama
        double bedDemand,       // llegadas que buscan cama ese día
        double occupancyRatio,  // camas ocupadas / capacidad
        double pressureRatio,   // (ocupadas + cola) / capacidad
        double rEffective,      // número reproductivo efectivo R_e(t)
        // --- diagnóstico de colas (Erlang-C, aproximación estacionaria) ---
        double offeredLoad,     // a = lambda / mu_s  (Erlangs)
        double rho,             // a / c
        double probWait,        // C(c, a) probabilidad de esperar
        double waitDaysQueue    // W_q en días (-1 si SATURADO / inestable)
) {}
