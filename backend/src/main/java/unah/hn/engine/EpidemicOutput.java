package unah.hn.engine;

public record EpidemicOutput(
        double[][] states,
        double[] dailyIncidence,
        double[] rEffective,
        double r0
) {}
