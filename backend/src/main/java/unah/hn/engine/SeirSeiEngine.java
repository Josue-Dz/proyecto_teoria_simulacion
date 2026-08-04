package unah.hn.engine;

import unah.hn.dto.Intervention;
import unah.hn.dto.InterventionType;
import unah.hn.dto.ModelParameters;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Motor epidemiológico SEIR-SEI acoplado (7 compartimentos: 4 humanos + 3 vector).
 * Integra el sistema de EDO con Runge-Kutta de 4.º orden, dt = 0.1 días,
 * muestreando el estado a paso diario. Las intervenciones se aplican como
 * modificadores dependientes del tiempo.
 */
@Component
public class SeirSeiEngine {

    // Índices del vector de estado
    public static final int SH = 0, EH = 1, IH = 2, RH = 3, SV = 4, EV = 5, IV = 6;

    private static final double DT = 0.1;
    private static final int SUBSTEPS = (int) Math.round(1.0 / DT); // 10 pasos por día

    /**
     * @param p                 parámetros del modelo (ya normalizados, sin nulls)
     * @param interventions      lista de intervenciones
     * @param horizon            horizonte en días
     * @param transmissionNoise  multiplicador de transmisión (1.0 = determinista)
     */
    public EpidemicOutput integrate(ModelParameters p, List<Intervention> interventions,
                                    int horizon, double transmissionNoise) {

        final double Nh = p.populationHuman();
        final double NvBase = p.vectorRatio() * Nh;
        final double b = p.bitingRate();
        final double betaH = p.betaHuman();
        final double betaV = p.betaVector();
        final double nuH = 1.0 / p.incubationHumanDays();
        final double nuV = 1.0 / p.incubationVectorDays();
        final double gammaH = 1.0 / p.infectiousHumanDays();
        final double muH = 1.0 / p.lifeExpectancyDaysHuman();
        final double muV = 1.0 / p.mosquitoLifespanDays();

        final double r0 = basicReproductionNumber(b, betaH, betaV, nuH, nuV, gammaH, muH, muV, Nh, NvBase);

        double[] y = new double[7];
        double i0 = p.initialInfectedHumans();
        double immune = clamp01(p.initialImmuneFraction()) * Nh;
        
        immune = Math.min(immune, Math.max(0.0, Nh - i0));
        y[RH] = immune;
        y[IH] = i0;
        y[SH] = Math.max(0.0, Nh - i0 - immune);
        y[SV] = NvBase;

        double[][] states = new double[horizon + 1][7];
        double[] incidence = new double[horizon + 1];
        double[] rEff = new double[horizon + 1];
        states[0] = y.clone();
        rEff[0] = r0 * Math.sqrt(safeRatio(y[SH], Nh) * safeRatio(y[SV], NvBase));

        for (int day = 0; day < horizon; day++) {
            // La fumigación vacía los compartimentos pero deja N_v intacta: mata al adulto,
            // no al criadero. El reclutamiento mu_v·N_v repuebla la población en unos 14
            // días, así que es un golpe transitorio que retrasa el pico.
            for (Intervention iv : interventions) {
                if (iv.type() == InterventionType.FUMIGATION
                        && iv.startDay() != null && iv.magnitude() != null
                        && iv.startDay() == day) {
                    double kill = clamp01(iv.magnitude());
                    y[SV] *= (1 - kill);
                    y[EV] *= (1 - kill);
                    y[IV] *= (1 - kill);
                }
            }

            // Modificador persistente: reducción de transmisión (+ ruido estocástico)
            double tMult = transmissionNoise;
            for (Intervention iv : interventions) {
                if (iv.type() == InterventionType.TRANSMISSION_REDUCTION
                        && iv.startDay() != null && iv.magnitude() != null
                        && day >= iv.startDay()) {
                    tMult *= (1 - clamp01(iv.magnitude()));
                }
            }
            double effBetaH = betaH * tMult;
            double effBetaV = betaV * tMult;

            // Integración de un día con RK4
            double dayIncidence = 0.0;
            for (int s = 0; s < SUBSTEPS; s++) {
                double ehBefore = y[EH];

                double[] k1 = deriv(y, b, effBetaH, effBetaV, nuH, nuV, gammaH, muH, muV, Nh, NvBase);
                double[] k2 = deriv(add(y, k1, DT / 2), b, effBetaH, effBetaV, nuH, nuV, gammaH, muH, muV, Nh, NvBase);
                double[] k3 = deriv(add(y, k2, DT / 2), b, effBetaH, effBetaV, nuH, nuV, gammaH, muH, muV, Nh, NvBase);
                double[] k4 = deriv(add(y, k3, DT), b, effBetaH, effBetaV, nuH, nuV, gammaH, muH, muV, Nh, NvBase);

                for (int c = 0; c < 7; c++) {
                    y[c] += (DT / 6.0) * (k1[c] + 2 * k2[c] + 2 * k3[c] + k4[c]);
                    if (y[c] < 0) y[c] = 0; // guarda numérica
                }
                // Incidencia (flujo E_h -> I_h) por trapecio sobre el subpaso
                dayIncidence += nuH * DT * (ehBefore + y[EH]) / 2.0;
            }

            states[day + 1] = y.clone();
            incidence[day + 1] = dayIncidence;
            rEff[day + 1] = r0 * Math.sqrt(safeRatio(y[SH], Nh) * safeRatio(y[SV], NvBase));
        }

        return new EpidemicOutput(states, incidence, rEff, r0);
    }

    /** Derivadas del sistema SEIR-SEI. */
    private double[] deriv(double[] y, double b, double betaH, double betaV,
                           double nuH, double nuV, double gammaH,
                           double muH, double muV, double Nh, double Nv) {
        double Sh = y[SH], Eh = y[EH], Ih = y[IH], Rh = y[RH];
        double Sv = y[SV], Ev = y[EV], Iv = y[IV];

        double infHuman = (b * betaH / Nh) * Sh * Iv; // fuerza de infección sobre humanos (por I_v)
        double infVector = (b * betaV / Nh) * Sv * Ih; // fuerza de infección sobre vectores (por I_h)

        double[] d = new double[7];
        d[SH] = muH * Nh - infHuman - muH * Sh;
        d[EH] = infHuman - (nuH + muH) * Eh;
        d[IH] = nuH * Eh - (gammaH + muH) * Ih;
        d[RH] = gammaH * Ih - muH * Rh;
        d[SV] = muV * Nv - infVector - muV * Sv;
        d[EV] = infVector - (nuV + muV) * Ev;
        d[IV] = nuV * Ev - muV * Iv;
        return d;
    }

    /**
     * R0 por matriz de próxima generación (ciclo humano -> mosquito -> humano):
     * R0 = sqrt( b^2 * betaH * betaV * nuH * nuV * Nv
     *            / ((nuH+muH)(gammaH+muH)(nuV+muV) * muV * Nh) ).
     */
    private double basicReproductionNumber(double b, double betaH, double betaV,
                                           double nuH, double nuV, double gammaH,
                                           double muH, double muV, double Nh, double Nv) {
        double numerator = b * b * betaH * betaV * nuH * nuV * Nv;
        double denominator = (nuH + muH) * (gammaH + muH) * (nuV + muV) * muV * Nh;
        return Math.sqrt(numerator / denominator);
    }

    private double[] add(double[] y, double[] k, double h) {
        double[] r = new double[7];
        for (int i = 0; i < 7; i++) r[i] = y[i] + h * k[i];
        return r;
    }

    private double clamp01(double v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }

    private double safeRatio(double a, double b) {
        return b <= 0 ? 0 : a / b;
    }
}
