package unah.hn.engine;

import unah.hn.dto.HospitalParameters;
import unah.hn.dto.Intervention;
import unah.hn.dto.InterventionType;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Motor hospitalario estocástico por eventos discretos. Cola multiservidor no
 * estacionaria de c camas, con llegadas de Poisson no homogéneo y servicios
 * exponenciales de media LOS, gobernada por una lista de eventos futuros.
 *
 * Devuelve el mismo HospitalOutput que el motor determinista, incluidas las métricas
 * de Erlang-C, que sirven para contrastar la simulación con la teoría de colas.
 */
@Component
public class HospitalDesEngine {

    private enum EventType { ARRIVAL, DEPARTURE, DETERIORATION, DEATH, ABANDON, CAPACITY }

    private static final class Patient {
        double arrivalTime;
        boolean admitted;
        boolean dead;
        boolean left;         // abandonó la cola al superar la espera máxima
        boolean grave;
        boolean vulnerable;

        /** Sigue esperando cama: ni ingresó, ni murió, ni se fue. */
        boolean enCola() {
            return !admitted && !dead && !left;
        }
    }

    private static final class Event implements Comparable<Event> {
        final double time;
        final EventType type;
        final Patient patient;
        Event(double time, EventType type, Patient patient) {
            this.time = time;
            this.type = type;
            this.patient = patient;
        }
        @Override
        public int compareTo(Event o) {
            return Double.compare(this.time, o.time);
        }
    }

    /** Estado mutable de la corrida (mantiene el motor sin campos de instancia). */
    private static final class SimState {
        int busy;
        int queueAlive;
        double waitSum;
        long waitCount;
    }

    public HospitalOutput simulate(double[] incidence, HospitalParameters hp,
                                   List<Intervention> interventions, int horizon, long seed) {

        final double pH = hp.hospitalizedFraction();
        final int tau = (int) Math.round(hp.admissionDelayDays());
        final double los = hp.avgStayDays();
        final double muS = 1.0 / los;
        final double threshold = hp.deteriorationThresholdDays();
        final double graveMortality = hp.graveMortalityPerDay();
        final double c0 = hp.initialBeds();
        final double severeFraction = Math.max(0.0, Math.min(1.0, hp.severeFraction()));
        final double maxWait = Math.max(threshold + 1.0, hp.maxWaitDays());

        final Rng rng = new Rng(seed);

        double[] occupied = new double[horizon + 1];
        double[] queue = new double[horizon + 1];
        double[] capacity = new double[horizon + 1];
        double[] admissions = new double[horizon + 1];
        double[] discharges = new double[horizon + 1];
        double[] deaths = new double[horizon + 1];
        double[] cumDeaths = new double[horizon + 1];
        double[] leftWithoutBed = new double[horizon + 1];
        double[] arrivalsPerDay = new double[horizon + 1];
        double[] offered = new double[horizon + 1];
        double[] rhoArr = new double[horizon + 1];
        double[] probWait = new double[horizon + 1];
        double[] wqArr = new double[horizon + 1];

        // Demanda esperada por día (overlay Erlang y muestreo de llegadas)
        double[] expectedDemand = new double[horizon + 1];
        for (int t = 1; t <= horizon; t++) {
            expectedDemand[t] = (t - tau >= 0) ? pH * incidence[t - tau] : 0.0;
        }
        for (int t = 1; t <= horizon; t++) {
            capacity[t] = capacityAt(c0, interventions, t);
        }
        capacity[0] = c0;

        PriorityQueue<Event> fel = new PriorityQueue<>();

        // Llegadas: proceso de Poisson no homogéneo, por día
        for (int d = 1; d <= horizon; d++) {
            int count = rng.poisson(expectedDemand[d]);
            arrivalsPerDay[d] = count;
            for (int i = 0; i < count; i++) {
                double t = (d - 1) + rng.uniform();
                Patient p = new Patient();
                p.arrivalTime = t;
                p.vulnerable = rng.uniform() < severeFraction;
                fel.add(new Event(t, EventType.ARRIVAL, p));
            }
        }
        // Aumentos de capacidad (camas extra)
        for (Intervention iv : interventions) {
            if (iv.type() == InterventionType.EXTRA_BEDS
                    && iv.startDay() != null && iv.magnitude() != null) {
                fel.add(new Event(Math.max(0.0, iv.startDay()), EventType.CAPACITY, null));
            }
        }

        Deque<Patient> waiting = new ArrayDeque<>();
        SimState st = new SimState();
        int nextSampleDay = 0;

        while (!fel.isEmpty()) {
            Event e = fel.poll();
            double now = e.time;

            if (now > horizon) break;

            while (nextSampleDay <= Math.floor(now) && nextSampleDay <= horizon) {
                occupied[nextSampleDay] = st.busy;
                queue[nextSampleDay] = st.queueAlive;
                nextSampleDay++;
            }

            int day = Math.max(0, Math.min((int) Math.floor(now), horizon));
            double cap = capacityAt(c0, interventions, day);

            switch (e.type) {
                case ARRIVAL -> {
                    if (st.busy < cap) {
                        e.patient.admitted = true;
                        st.busy++;
                        admissions[day] += 1;
                        fel.add(new Event(now + rng.exponential(los), EventType.DEPARTURE, e.patient));
                    } else {
                        waiting.addLast(e.patient);
                        st.queueAlive++;
                        if (e.patient.vulnerable) {
                            fel.add(new Event(now + threshold, EventType.DETERIORATION, e.patient));
                        }

                        fel.add(new Event(now + maxWait, EventType.ABANDON, e.patient));
                    }
                }
                case DEPARTURE -> {
                    st.busy--;
                    discharges[day] += 1;
                    admitFromQueue(now, day, cap, st, waiting, fel, los, rng, admissions);
                }
                case DETERIORATION -> {
                    Patient p = e.patient;
                    if (p.enCola()) {
                        p.grave = true;
                        double meanTimeToDeath = 1.0 / Math.max(1e-9, graveMortality);
                        fel.add(new Event(now + rng.exponential(meanTimeToDeath), EventType.DEATH, p));
                    }
                }
                case DEATH -> {
                    Patient p = e.patient;
                    if (p.enCola()) {
                        p.dead = true;
                        st.queueAlive--;
                        deaths[day] += 1;
                    }
                }
                case ABANDON -> {
                    Patient p = e.patient;
                    if (p.enCola()) {
                        p.left = true;
                        st.queueAlive--;
                        leftWithoutBed[day] += 1;
                    }
                }
                case CAPACITY -> admitFromQueue(now, day, cap, st, waiting, fel, los, rng, admissions);
            }
        }

        while (nextSampleDay <= horizon) {
            occupied[nextSampleDay] = st.busy;
            queue[nextSampleDay] = st.queueAlive;
            nextSampleDay++;
        }

        // Muertes acumuladas + overlay Erlang-C
        double cum = 0.0;
        double totalLeft = 0.0;
        for (int t = 0; t <= horizon; t++) {
            cum += deaths[t];
            cumDeaths[t] = cum;
            totalLeft += leftWithoutBed[t];
            double cap = capacity[t] > 0 ? capacity[t] : 1;
            double aLoad = expectedDemand[t] * los;
            int servers = (int) Math.max(1, Math.round(cap));
            double rho = aLoad / cap;
            double pw = ErlangUtil.erlangC(servers, aLoad);
            offered[t] = aLoad;
            rhoArr[t] = rho;
            probWait[t] = pw;
            wqArr[t] = (rho < 1.0) ? pw / (cap * muS - expectedDemand[t]) : -1.0;
        }

        double totalArrivals = 0.0;
        for (int t = 0; t <= horizon; t++) totalArrivals += arrivalsPerDay[t];
        double avgWait = st.waitCount > 0 ? st.waitSum / st.waitCount : 0.0;

        return new HospitalOutput(occupied, queue, capacity, admissions, discharges,
                deaths, cumDeaths, leftWithoutBed, arrivalsPerDay, offered, rhoArr, probWait, wqArr,
                cum, totalLeft, st.queueAlive, avgWait, totalArrivals);
    }

    /** Admite de la cola mientras haya camas libres (FIFO). */
    private void admitFromQueue(double now, int day, double cap, SimState st,
                                Deque<Patient> waiting, PriorityQueue<Event> fel,
                                double los, Rng rng, double[] admissions) {
        while (st.busy < cap && !waiting.isEmpty()) {
            Patient p = waiting.pollFirst();
            // La cola puede contener pacientes que ya murieron o abandonaron, se descartan.
            if (p == null || !p.enCola()) {
                continue;
            }
            p.admitted = true;
            st.queueAlive--;
            st.busy++;
            st.waitSum += now - p.arrivalTime;
            st.waitCount++;
            admissions[day] += 1;
            fel.add(new Event(now + rng.exponential(los), EventType.DEPARTURE, p));
        }
    }

    private double capacityAt(double base, List<Intervention> interventions, int day) {
        double cap = base;
        for (Intervention iv : interventions) {
            if (iv.type() == InterventionType.EXTRA_BEDS
                    && iv.startDay() != null && iv.magnitude() != null
                    && day >= iv.startDay()) {
                cap += iv.magnitude();
            }
        }
        return cap;
    }
}
