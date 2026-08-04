package unah.hn.engine;

import unah.hn.dto.HospitalParameters;
import unah.hn.dto.Intervention;
import unah.hn.dto.InterventionType;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Motor determinista de capacidad hospitalaria. Las camas son servidores finitos y la
 * cola se atiende por antigüedad de espera.
 *
 * Se llevan dos cohortes porque no todos corren el mismo riesgo: los vulnerables se
 * agravan al superar el umbral θ y mueren con tasa diaria δ mientras no reciban cama,
 * mientras que los moderados se recuperan con manejo básico. Unos y otros abandonan la
 * cola al superar la espera máxima W_max.
 */
@Component
public class HospitalEngine {

    /** @param incidence incidencia diaria que produce SeirSeiEngine */
    public HospitalOutput simulate(double[] incidence, HospitalParameters hp,
                                   List<Intervention> interventions, int horizon) {

        final double pH = hp.hospitalizedFraction();
        final int tau = (int) Math.round(hp.admissionDelayDays());
        final double los = hp.avgStayDays();
        final double muS = 1.0 / los;                 // tasa de servicio por cama
        final int graveThreshold = Math.max(1, (int) Math.round(hp.deteriorationThresholdDays()));
        final double graveMortality = hp.graveMortalityPerDay();
        final int maxWait = Math.max(graveThreshold + 1, (int) Math.round(hp.maxWaitDays()));
        final double c0 = hp.initialBeds();
        // Solo la fracción vulnerable está en riesgo de morir por la espera.
        final double severeFraction = clamp01(hp.severeFraction());

        // Fracción de camas que se liberan en un día con servicio exponencial de media LOS.
        // Es 1 − e^(−μ_s) y no μ_s: a paso diario el Euler explícito sobreestima las altas,
        // y con LOS < 1 día daría de alta a más pacientes de los que hay ingresados.
        final double dischargeFraction = 1.0 - Math.exp(-muS);

        double[] occupied = new double[horizon + 1];
        double[] queue = new double[horizon + 1];
        double[] capacity = new double[horizon + 1];
        double[] admissions = new double[horizon + 1];
        double[] discharges = new double[horizon + 1];
        double[] deaths = new double[horizon + 1];
        double[] cumDeaths = new double[horizon + 1];
        double[] leftWithoutBed = new double[horizon + 1];
        double[] demandArr = new double[horizon + 1];
        double[] offered = new double[horizon + 1];
        double[] rhoArr = new double[horizon + 1];
        double[] probWait = new double[horizon + 1];
        double[] wqArr = new double[horizon + 1];

        // Cola por antigüedad: q[a] = pacientes que llevan 'a' días esperando.
        double[] qVulnerable = new double[maxWait + 1];
        double[] qModerate = new double[maxWait + 1];
        double occ = 0.0;
        double cumulativeDeaths = 0.0;
        double totalLeftWithoutBed = 0.0;
        double totalWaitDays = 0.0;   // suma de (edad * admitidos) para la espera media
        double countWaited = 0.0;     // admitidos que pasaron por la cola
        double totalDemand = 0.0;

        capacity[0] = c0;

        for (int t = 1; t <= horizon; t++) {
            double cap = capacityAt(c0, interventions, t);

            // Llegadas del día = fracción hospitalizada de la incidencia retardada tau días
            double demand = (t - tau >= 0) ? pH * incidence[t - tau] : 0.0;
            totalDemand += demand;

            // 1) Altas (servicio exponencial de media LOS)
            double disch = occ * dischargeFraction;
            occ -= disch;

            // 2) Envejecer la cola un día. Los que ya llevaban W_max días esperando
            //    abandonan: nunca recibieron cama y dejan de ocupar la cola.
            double leftToday = qVulnerable[maxWait] + qModerate[maxWait];
            double[] nVul = new double[maxWait + 1];
            double[] nMod = new double[maxWait + 1];
            for (int a = 0; a < maxWait; a++) {
                nVul[a + 1] = qVulnerable[a];
                nMod[a + 1] = qModerate[a];
            }
            // Las llegadas del día entran con edad 0, repartidas por nivel de riesgo.
            nVul[0] = demand * severeFraction;
            nMod[0] = demand * (1.0 - severeFraction);

            // 3) Mortalidad de los vulnerables ya agravados (edad >= umbral de deterioro)
            //    que siguen sin cama. Los moderados no mueren por esperar.
            double deathsToday = 0.0;
            for (int a = graveThreshold; a <= maxWait; a++) {
                double d = nVul[a] * graveMortality;
                nVul[a] -= d;
                deathsToday += d;
            }

            // 4) Admitir a camas libres, primero los que más han esperado (FIFO).
            //    Dentro de una misma edad se reparte proporcionalmente entre cohortes:
            //    lo que da prioridad es la antigüedad, no el nivel de riesgo.
            double free = Math.max(0.0, cap - occ);
            double admittedToday = 0.0;
            for (int a = maxWait; a >= 0 && free > 1e-9; a--) {
                double enEsaEdad = nVul[a] + nMod[a];
                if (enEsaEdad <= 1e-12) continue;
                double take = Math.min(free, enEsaEdad);
                double proporcionVulnerable = nVul[a] / enEsaEdad;
                nVul[a] -= take * proporcionVulnerable;
                nMod[a] -= take * (1.0 - proporcionVulnerable);
                occ += take;
                free -= take;
                admittedToday += take;
                totalWaitDays += take * a;
                countWaited += take;
            }

            double queueLen = sum(nVul) + sum(nMod);

            // 5) Diagnóstico Erlang-C del día (aproximación estacionaria puntual)
            double aLoad = demand * los;              // a = lambda / mu_s
            int servers = (int) Math.max(1, Math.round(cap));
            double rho = aLoad / cap;
            double pw = ErlangUtil.erlangC(servers, aLoad);
            double wq = (rho < 1.0) ? pw / (cap * muS - demand) : -1.0; // -1 = SATURADO

            // Registro
            occupied[t] = occ;
            queue[t] = queueLen;
            capacity[t] = cap;
            admissions[t] = admittedToday;
            discharges[t] = disch;
            deaths[t] = deathsToday;
            cumulativeDeaths += deathsToday;
            cumDeaths[t] = cumulativeDeaths;
            leftWithoutBed[t] = leftToday;
            totalLeftWithoutBed += leftToday;
            demandArr[t] = demand;
            offered[t] = aLoad;
            rhoArr[t] = rho;
            probWait[t] = pw;
            wqArr[t] = wq;

            qVulnerable = nVul;
            qModerate = nMod;
        }

        double finalQueue = sum(qVulnerable) + sum(qModerate);
        double avgWait = countWaited > 0 ? totalWaitDays / countWaited : 0.0;

        return new HospitalOutput(occupied, queue, capacity, admissions, discharges,
                deaths, cumDeaths, leftWithoutBed, demandArr, offered, rhoArr, probWait, wqArr,
                cumulativeDeaths, totalLeftWithoutBed, finalQueue, avgWait, totalDemand);
    }

    /** Capacidad en el día t: camas iniciales + camas extra habilitadas hasta t. */
    private double capacityAt(double base, List<Intervention> interventions, int t) {
        double cap = base;
        for (Intervention iv : interventions) {
            if (iv.type() == InterventionType.EXTRA_BEDS
                    && iv.startDay() != null && iv.magnitude() != null
                    && t >= iv.startDay()) {
                cap += iv.magnitude();
            }
        }
        return cap;
    }

    private double sum(double[] a) {
        double s = 0;
        for (double v : a) s += v;
        return s;
    }

    private double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }
}
