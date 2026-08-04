package unah.hn.engine;

/**
 * Salida del motor hospitalario. Todos los arreglos tienen longitud horizon+1.
 *
 * Un paciente que llega buscando cama termina en uno de cuatro estados:
 * ingresa (admissions), muere esperando (deaths), abandona la cola al superar la
 * espera máxima sin haber recibido cama (leftWithoutBed), o sigue en cola al
 * terminar el horizonte (finalQueue).
 */
public record HospitalOutput(
        double[] occupied,
        double[] queue,
        double[] capacity,
        double[] admissions,
        double[] discharges,
        double[] deaths,
        double[] cumulativeDeaths,
        double[] leftWithoutBed,
        double[] demand,
        double[] offeredLoad,
        double[] rho,
        double[] probWait,
        double[] waitDaysQueue,
        double totalQueueDeaths,
        double totalLeftWithoutBed,
        double finalQueue,
        double avgWaitDays,
        double totalHospitalizations
) {}
