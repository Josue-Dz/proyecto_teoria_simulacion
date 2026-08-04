package unah.hn.engine;

/**
 * Teoría de colas M/M/c como diagnóstico estacionario puntual. No describe la
 * trayectoria, que es cosa de HospitalEngine, sino la presión instantánea bajo la
 * carga de llegadas de un día concreto.
 */
public final class ErlangUtil {

    private ErlangUtil() {}

    /**
     * Probabilidad de espera de Erlang-C, C(c, a).
     * @param servers número de camas
     * @param offeredLoad carga ofrecida en Erlangs, lambda / mu_s
     */
    public static double erlangC(int servers, double offeredLoad) {
        if (offeredLoad <= 0) return 0.0;
        if (servers <= 0) return 1.0;
        if (offeredLoad >= servers) return 1.0; // sistema inestable: siempre se espera

        // Erlang-B por recurrencia numéricamente estable: B(0)=1
        double b = 1.0;
        for (int n = 1; n <= servers; n++) {
            b = (offeredLoad * b) / (n + offeredLoad * b);
        }
        double rho = offeredLoad / servers;
        return b / (1.0 - rho + rho * b);
    }
}
