package unah.hn.engine;

import java.util.Random;

/**
 * Variables aleatorias para la simulación por eventos discretos. La semilla fija hace
 * reproducible cada corrida.
 */
public class Rng {

    private final Random random;

    public Rng(long seed) {
        this.random = new Random(seed);
    }

    /** U(0,1). */
    public double uniform() {
        return random.nextDouble();
    }

    /**
     * Exponencial por transformada inversa: X = -media * ln(1 - U).
     * @param mean valor esperado de la variable (p. ej. LOS para el servicio)
     */
    public double exponential(double mean) {
        double u = random.nextDouble();
        return -mean * Math.log(1.0 - u);
    }

    /**
     * Multiplicador lognormal de media 1 y dispersión sigma, para dar incertidumbre a un
     * parámetro sin que cambie de signo ni se desplace su valor esperado.
     */
    public double logNormalMultiplier(double sigma) {
        if (sigma <= 0) return 1.0;
        return Math.exp(sigma * random.nextGaussian() - 0.5 * sigma * sigma);
    }

    /**
     * Poisson(lambda) por el método multiplicativo de Knuth, que es O(lambda), con
     * aproximación normal a partir de 30 para el pico del brote, donde lambda llega
     * a valer miles.
     */
    public int poisson(double lambda) {
        if (lambda <= 0) return 0;
        if (lambda < 30) {
            double l = Math.exp(-lambda);
            int k = 0;
            double p = 1.0;
            do {
                k++;
                p *= random.nextDouble();
            } while (p > l);
            return k - 1;
        }
        double value = lambda + Math.sqrt(lambda) * random.nextGaussian();
        return Math.max(0, (int) Math.round(value));
    }
}
